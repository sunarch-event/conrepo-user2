package com.performance.domain.dao;

import java.util.List;

import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.performance.domain.entity.UserHobby;
import com.performance.domain.entity.UserInfo;
import com.performance.domain.entity.UserMaster;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Repository
public class UserDao {

    final static Logger log = LogManager.getLogger(UserDao.class);

    private JdbcTemplate jdbcTemplate;
    
    public UserDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public Long insertUserInfo (UserInfo entity) {
        String sql = "INSERT INTO user_info (last_name, first_name, prefectures, city, blood_type)";
        sql = sql + " VALUES (";
        sql = sql + "'" + entity.getLastName() + "', ";
        sql = sql + "'" + entity.getFirstName() + "', ";
        sql = sql + "'" + entity.getPrefectures() + "', ";
        sql = sql + "'" + entity.getCity() + "', ";
        sql = sql + "'" + entity.getBloodType() + "') ";
        // 主キーを取得できるように修正
        sql = sql + "RETURNING id";
        //jdbcTemplate.execute(sql);
        return jdbcTemplate.queryForObject(sql, Long.class);
    }
        
    @Transactional
    public void insertUserHobby (UserHobby entity) {
        String sql = "INSERT INTO user_hobby (id, hobby1, hobby2, hobby3, hobby4, hobby5)";
        sql = sql + " VALUES (";
        sql = sql + "'" + entity.getId() + "', ";
        sql = sql + "'" + entity.getHobby1() + "', ";
        sql = sql + "'" + entity.getHobby2() + "', ";
        sql = sql + "'" + entity.getHobby3() + "', ";
        sql = sql + "'" + entity.getHobby4() + "', ";
        sql = sql + "'" + entity.getHobby5() + "')";
        jdbcTemplate.execute(sql);
    }

    public Long selectId(UserInfo entity) {
        String sql = "SELECT id ";
        sql = sql + "FROM user_info ";
        // 一旦コメントアウト
        //sql = sql + "WHERE last_name || first_name = " + "'" + entity.getLastName() + entity.getFirstName() + "'";
        
        sql = sql + "WHERE last_name = " + "'" + entity.getLastName() + "'";
        sql = sql + "AND first_name = " + "'" + entity.getFirstName() + "'";
        sql = sql + " ORDER BY id desc";
        sql = sql + " LIMIT 1";
        return jdbcTemplate.queryForObject(sql, Long.class);
    }

    public List<UserInfo> searchUserInfo() {
        String sql = "SELECT id, last_name, first_name, prefectures, city, blood_type ";
        sql = sql + "FROM user_info ";
        // 一旦コメントアウト
        //sql = sql + "WHERE last_name || first_name <> " + "'試験太郎'";

        sql = sql + "WHERE last_name <> " + "'試験'";
        sql = sql + "OR first_name <> " + "'太郎'"; 
        sql = sql + " ORDER BY id";
        RowMapper<UserInfo> mapper = new BeanPropertyRowMapper<UserInfo>(UserInfo.class);
        return jdbcTemplate.query(sql, mapper);
    }

    public List<UserHobby> searchUserHobby(UserHobby targetUserHobby) {
        String sql = "SELECT id, hobby1, hobby2, hobby3, hobby4, hobby5 ";
        sql = sql + "FROM user_hobby ";
        sql = sql + "WHERE id  <> " + targetUserHobby.getId();
        sql = sql + " ORDER BY id";
        RowMapper<UserHobby> mapper = new BeanPropertyRowMapper<UserHobby>(UserHobby.class);
        return jdbcTemplate.query(sql, mapper);
    }

    public List<UserMaster> searchUserMaster() {
        String sql = "SELECT info.id id, info.last_name last_name, info.first_name first_name, info.prefectures prefectures, info.city city, info.blood_type blood_type, ";
        sql = sql + "hobby.hobby1 hobby1, hobby.hobby2 hobby2, hobby.hobby3 hobby3, hobby.hobby4 hobby4, hobby.hobby5 hobby5 ";
        sql = sql + "FROM user_info info, user_hobby hobby ";
        sql = sql + "WHERE info.id = hobby.id ";
        sql = sql + "AND info.last_name || info.first_name <> " + "'試験太郎'";
        sql = sql + " ORDER BY info.id";
        RowMapper<UserMaster> mapper = new BeanPropertyRowMapper<UserMaster>(UserMaster.class);
        return jdbcTemplate.query(sql, mapper);
    }
    
    public UserInfo getTargetUserInfo() {
        String sql = "SELECT id, last_name, first_name, prefectures, city, blood_type ";
        sql = sql + "FROM user_info ";
        sql = sql + "WHERE last_name = " + "'試験'";
        sql = sql + "AND first_name = " + "'太郎'";
        RowMapper<UserInfo> mapper = new BeanPropertyRowMapper<UserInfo>(UserInfo.class);
        return jdbcTemplate.queryForObject(sql, mapper);
    }
    
    public UserHobby getTargetUserHobby(UserInfo userInfo) {
        String sql = "SELECT id, hobby1, hobby2, hobby3, hobby4, hobby5 ";
        sql = sql + "FROM user_hobby ";
        sql = sql + "WHERE id = " + userInfo.getId();
        RowMapper<UserHobby> mapper = new BeanPropertyRowMapper<UserHobby>(UserHobby.class);
        return jdbcTemplate.queryForObject(sql, mapper);
    }

    public UserMaster getTargetUserData() {
        UserMaster targetUserMaster = new UserMaster();
        String sql = "SELECT info.id id, info.last_name last_name, info.first_name first_name, info.prefectures prefectures, info.city city, info.blood_type blood_type, ";
        sql = sql + "hobby.hobby1 hobby1, hobby.hobby2 hobby2, hobby.hobby3 hobby3, hobby.hobby4 hobby4, hobby.hobby5 hobby5 ";
        sql = sql + "FROM user_info info, user_hobby hobby ";
        sql = sql + "WHERE info.id = hobby.id ";
        sql = sql + "AND info.last_name = " + "'試験' ";
        sql = sql + "AND info.first_name = " + "'太郎' ";

        RowMapper<UserMaster> mapper = new BeanPropertyRowMapper<UserMaster>(UserMaster.class);
        targetUserMaster = jdbcTemplate.queryForObject(sql, mapper);
        
        return targetUserMaster;
        //RowMapper<UserMaster> mapper = new BeanPropertyRowMapper<UserMaster>(UserMaster.class);
        //return jdbcTemplate.queryForObject(sql, mapper);
    }
    
    public int searchCount() {
        String sql = "SELECT COUNT(*) FROM user_info";
        
        return jdbcTemplate.queryForObject(sql, Integer.class);
    }

    public void truncateUserInfo() {
        String sql = "TRUNCATE TABLE user_info";
        jdbcTemplate.execute(sql);
    }
    
    public void truncateUserHobby() {
        String sql = "TRUNCATE TABLE user_hobby";
        jdbcTemplate.execute(sql);
    }
    
}
