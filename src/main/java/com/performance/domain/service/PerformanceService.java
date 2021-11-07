package com.performance.domain.service;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.performance.domain.dao.UserDao;
import com.performance.domain.entity.UserHobby;
import com.performance.domain.entity.UserInfo;
import com.performance.domain.entity.UserMaster;

@Service
public class PerformanceService {

    final static Logger log = LogManager.getLogger(PerformanceService.class);

    private final String MEASURE_FLAG_ON  = "1";

    private GoogleApiService googleService;

    private UserDao userDao;
    
    private Map<String, Long> resultMap = new HashMap<String, Long>();
    private Map<String, Boolean> assertionResultMap = new HashMap<String, Boolean>();

    public PerformanceService(GoogleApiService googleService, UserDao userDao) {
        this.googleService = googleService;
        this.userDao = userDao;
    }

    @Async("perfomanceExecutor")
    public void execute(String uuid, String measureFlag) {

        resultMap.clear();
        resultMap.put(uuid, null);

        

        Long start = System.currentTimeMillis();

        List<UserMaster> matchingUserList = uploadExecute();

        Long end = System.currentTimeMillis();
        Long executeTime = end - start;

        resultMap.put(uuid, executeTime);
        // アサーション入れる
        Boolean assertionResult = assertion(matchingUserList);
        assertionResultMap.put(uuid, assertionResult);
        
        // 計測実施かつアサーションが成功している場合のみ送る
        if(MEASURE_FLAG_ON.equals(measureFlag) && assertionResult) {
            try {
                googleService.execute(executeTime);
            } catch (Exception e) {
                log.error("スプレッドシートの更新でエラーが発生しました。", e);
            }
        }
        return;
    }
    public List<UserMaster> uploadExecute() {
        // テーブル情報を空にする
        /** 変更不可 **/
        truncateTable();
        /** 変更不可 **/
        
        // CSVを取得・CSVファイルをDBに登録する
        //ファイル読み込みで使用する3つのクラス
        BufferedReader br = null;
        List<String> csvFile = new ArrayList<String>();
        try {

            //読み込みファイルのインスタンス生成
            //ファイル名を指定する

            // ファイル読み込み方式を修正
            FileInputStream fIStream= new FileInputStream("data/userInfo.csv");
            InputStreamReader iSReader = new InputStreamReader(fIStream);
            br = new BufferedReader(iSReader);
            

            //読み込み行
            String readLine;

            //読み込み行数の管理
            int i = 0;

            //1行ずつ読み込みを行う
            while ((readLine = br.readLine()) != null) {
                i++;
                //データ内容をコンソールに表示する
                log.info("-------------------------------");

                //データ件数を表示
                log.info("データ読み込み" + i + "件目");
                
                csvFile.add(readLine);
            }
        } catch (Exception e) {
            log.info("csv read error", e);
        } finally {
            try {
                br.close();
            } catch (Exception e) {
            }
        }

        try {
            int i = 0;
            // CSV分割用
            Pattern csvPtn = Pattern.compile(",");
            for(String line : csvFile) {
                //カンマで分割した内容を配列に格納する
                String[] data = csvPtn.split(line, -1);
                //データ内容をコンソールに表示する
                log.info("-------------------------------");
                //データ件数を表示
                //配列の中身を順位表示する。列数(=列名を格納した配列の要素数)分繰り返す
                log.debug("ユーザー姓:" + data[1]);
                log.debug("出身都道府県:" + data[2]);
                log.debug("ユーザー名:" + data[0]);
                log.debug("出身市区町村:" + data[3]);
                log.debug("血液型:" + data[4]);
                log.debug("趣味1:" + data[5]);
                log.debug("趣味2:" + data[6]);
                log.debug("趣味3:" + data[7]);
                log.debug("趣味4:" + data[8]);
                log.debug("趣味5:" + data[9]);

                // 特定の件のみインサートするようにする
                Pattern pattern = Pattern.compile(".新潟県,上越市.");
                Matcher matcher = pattern.matcher(line);
                if(matcher.find()) {
                    // 行数のインクリメント
                    i++;
                    
                    // インサートする場合のみ、値をセットする
                    UserInfo userInfo = new UserInfo();
                    UserHobby userHobby = new UserHobby();
                    userInfo.setLastName(data[0]);
                    userInfo.setFirstName(data[1]);
                    userInfo.setPrefectures(data[2]);
                    userInfo.setCity(data[3]);
                    userInfo.setBloodType(data[4]);
                    userHobby.setHobby1(data[5]);
                    userHobby.setHobby2(data[6]);
                    userHobby.setHobby3(data[7]);
                    userHobby.setHobby4(data[8]);
                    userHobby.setHobby5(data[9]);

                    log.info("データ書き込み" + i + "件目");

                    // DB登録時にidを取得するよう修正
                    Long id = userDao.insertUserInfo(userInfo);
                    userHobby.setId(id);
                    userDao.insertUserHobby(userHobby);
                }
            }

        } catch (Exception e) {
            log.info("csv read error", e);
        }
        // 対象情報取得
        UserInfo targetUserInfo = userDao.getTargetUserInfo();
        UserHobby targetUserHobby = userDao.getTargetUserHobby(targetUserInfo);
        UserMaster targetUserMaster = new UserMaster();
        
        targetUserMaster.setId(targetUserInfo.getId());
        targetUserMaster.setLastName(targetUserInfo.getLastName());
        targetUserMaster.setFirstName(targetUserInfo.getFirstName());
        targetUserMaster.setPrefectures(targetUserInfo.getPrefectures());
        targetUserMaster.setCity(targetUserInfo.getCity());
        targetUserMaster.setBloodType(targetUserInfo.getBloodType());
        targetUserMaster.setHobby1(targetUserHobby.getHobby1());
        targetUserMaster.setHobby2(targetUserHobby.getHobby2());
        targetUserMaster.setHobby3(targetUserHobby.getHobby3());
        targetUserMaster.setHobby4(targetUserHobby.getHobby4());
        targetUserMaster.setHobby5(targetUserHobby.getHobby5());
        
        // DBから検索する
        List<UserInfo> userInfoList = userDao.searchUserInfo();
        List<UserHobby> userHobbyList = userDao.searchUserHobby(targetUserHobby);
        
        List<UserMaster> userMasterList = new ArrayList<UserMaster>();

        // それぞれのリストサイズを事前に取得
        int infoListSize = userInfoList.size();
        int hobbyListSize = userHobbyList.size();
        
        for(int i = 0; i < infoListSize; i++) {
            UserMaster userMaster = new UserMaster();
            userMaster.setId(userInfoList.get(i).getId());
            userMaster.setLastName(userInfoList.get(i).getLastName());
            userMaster.setFirstName(userInfoList.get(i).getFirstName());
            userMaster.setPrefectures(userInfoList.get(i).getPrefectures());
            userMaster.setCity(userInfoList.get(i).getCity());
            userMaster.setBloodType(userInfoList.get(i).getBloodType());
            for(int j = 0; j < hobbyListSize; j++) {
                if(userMaster.getId().equals(userHobbyList.get(j).getId())) {
                    userMaster.setHobby1(userHobbyList.get(j).getHobby1());
                    userMaster.setHobby2(userHobbyList.get(j).getHobby2());
                    userMaster.setHobby3(userHobbyList.get(j).getHobby3());
                    userMaster.setHobby4(userHobbyList.get(j).getHobby4());
                    userMaster.setHobby5(userHobbyList.get(j).getHobby5());
                    break;
                }
            }
            userMasterList.add(userMaster);
        }

        List<UserMaster> matchingUserList = new ArrayList<UserMaster>();

        // HashSetに指定ユーザの情報をセット
        HashSet<String> targetHobbySet = new HashSet<String>();
        targetHobbySet.add(targetUserMaster.getHobby1());
        targetHobbySet.add(targetUserMaster.getHobby2());
        targetHobbySet.add(targetUserMaster.getHobby3());
        targetHobbySet.add(targetUserMaster.getHobby4());
        targetHobbySet.add(targetUserMaster.getHobby5());

        for(UserMaster user : userMasterList) {
            // 同じ血液型ユーザー
            if(user.getBloodType().equals(targetUserMaster.getBloodType())) {
                // 趣味1に同じ趣味を持っているユーザー
                if(targetHobbySet.contains(user.getHobby1())) {
                    matchingUserList.add(user);
                    continue;
                }
                // 趣味2に同じ趣味を持っているユーザー
                if(targetHobbySet.contains(user.getHobby2())) {
                    matchingUserList.add(user);
                    continue;
                }
                // 趣味3に同じ趣味を持っているユーザー
                if(targetHobbySet.contains(user.getHobby3())) {
                    matchingUserList.add(user);
                    continue;
                }
                // 趣味4に同じ趣味を持っているユーザー
                if(targetHobbySet.contains(user.getHobby4())) {
                    matchingUserList.add(user);
                    continue;
                }
                // 趣味5に同じ趣味を持っているユーザー
                if(targetHobbySet.contains(user.getHobby5())) {
                    matchingUserList.add(user);
                    continue;
                }
            }
        }
        return matchingUserList;
    }

    
    public void truncateTable() {
        userDao.truncateUserInfo();
        userDao.truncateUserHobby();
    }

    public Long referenceExecuteTime(String uuid) {
        
        Long result = null;
        if(resultMap.containsKey(uuid)) {
            result = resultMap.get(uuid);
        }
        
        return result;
    }
    
    public String referenceUuid() {
        
        String uuid = null;
        
        for(String key : resultMap.keySet()) {
            uuid = key;
        }
        
        return uuid;
    }

    private Boolean assertion(List<UserMaster> matchingUserList) {
        Boolean assertionResult = true;
        
        int count = userDao.searchCount();
        
        if(count != 10000) {
            return false;
        }
        
        if(matchingUserList.size() != 2072) {
            return false;
        }
        
        // CSVを取得・CSVファイルをDBに登録する
        //ファイル読み込みで使用する3つのクラス
        BufferedReader br = null;
        List<String> csvFile = new ArrayList<String>();
        try {

            //読み込みファイルのインスタンス生成
            //ファイル名を指定する
            
            // CSV読み込み処理の修正
            FileInputStream fIStream= new FileInputStream("data/assertionData.csv");
            InputStreamReader iSReader = new InputStreamReader(fIStream);
            br = new BufferedReader(iSReader);

            //読み込み行
            String readLine;
            //1行ずつ読み込みを行う
            while ((readLine = br.readLine()) != null) {
                csvFile.add(readLine);
            }
        } catch (Exception e) {
            log.info("csv read error", e);
        } finally {
            try {
                br.close();
            } catch (Exception e) {
            }
        }
        for(String line : csvFile) {
            boolean exsits = false;
            UserMaster userMaster = new UserMaster();
            String[] data = line.split(",", -1);

            userMaster.setLastName(data[0]);
            userMaster.setFirstName(data[1]);
            userMaster.setPrefectures(data[2]);
            userMaster.setCity(data[3]);
            userMaster.setBloodType(data[4]);
            userMaster.setHobby1(data[5]);
            userMaster.setHobby2(data[6]);
            userMaster.setHobby3(data[7]);
            userMaster.setHobby4(data[8]);
            userMaster.setHobby5(data[9]);
            for(UserMaster user : matchingUserList) {
                if(user.toString().equals(userMaster.toString())) {
                    exsits = true;
                    break;
                }
            }
            if(!exsits) {
                assertionResult = false;
            }
        }
        truncateTable();
        return assertionResult;
    }

    public Boolean referenceAssertionResult(String uuid) {
        Boolean assertionResult = assertionResultMap.get(uuid);
        return assertionResult;
    }
}
