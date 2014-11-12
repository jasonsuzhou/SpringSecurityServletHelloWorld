package com.mh.model.user.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.authentication.LockedException;
import org.springframework.stereotype.Repository;

import com.mh.model.user.UserAttempts;

@Repository
public class UserDetailsDaoImpl implements UserDetailsDao {

	private static final int MAX_ATTEMPTS = 3;
	private static final String SQL_USERS_UPDATE_LOCKED = "update users set accountNonLocked = ? where username = ?";
	private static final String SQL_USERS_COUNT = "select count(*) from users where username = ?";
	private static final String SQL_USERS_ATTEMPTS_GET = "select * from user_attempts where username = ?";
	private static final String SQL_USERS_ATTEMPTS_INSERT = "insert into user_attempts(username,attempts,lastModified) values (?,?,?)";
	private static final String SQL_USERS_UPDATE_ATTEMPTS = "update user_attempts set attempts = attempts+1 where username = ?";
	private static final String SQL_USERS_RESET_ATTEMPTS = "update user_attempts set attempts=0 where username = ?";

	@Autowired
	private JdbcTemplate jdbcTemplate;

	public void updateFailAttempts(String username) {
		UserAttempts user = getUserAttempts(username);
		if (user == null) {
			if (isUserExists(username)) {
				// if no record, insert a new
				jdbcTemplate.update(SQL_USERS_ATTEMPTS_INSERT, new Object[] { username, 1, new Date() });
			}
		} else {

			if (isUserExists(username)) {
				// update attempts count, +1
				jdbcTemplate.update(SQL_USERS_UPDATE_ATTEMPTS, new Object[] { username });
			}

			if (user.getAttempts() + 1 >= MAX_ATTEMPTS) {
				// locked user
				jdbcTemplate.update(SQL_USERS_UPDATE_LOCKED, new Object[] { false, username });
				// throw exception
				throw new LockedException("User Account is locked!");
			}

		}
	}

	public void resetFailAttempts(String username) {
		Object[] args = new Object[]{username};
		int[] argTypes = new int[]{Types.VARCHAR};
		this.jdbcTemplate.update(SQL_USERS_RESET_ATTEMPTS, args, argTypes);
	}

	public UserAttempts getUserAttempts(String username) {
		Object[] args = new Object[]{username};
		int[] argTypes = new int[]{Types.VARCHAR};
		try {
			UserAttempts user =  this.jdbcTemplate.queryForObject(SQL_USERS_ATTEMPTS_GET, args, argTypes, new RowMapper<UserAttempts>() {
	
				@Override
				public UserAttempts mapRow(ResultSet rs, int rowNum)
						throws SQLException {
					UserAttempts userAttempts = new UserAttempts();
					userAttempts.setId(rs.getInt("id"));
					userAttempts.setUsername(rs.getString("username"));
					userAttempts.setLastModified(rs.getDate("lastModified"));
					userAttempts.setAttempts(rs.getInt("attempts"));
					return userAttempts;
				}
			});
			return user;
		} catch (EmptyResultDataAccessException e) {
			return null;
		}
	}
	
	private boolean isUserExists(String username) {
		Object[] args = new Object[]{username};
		int[] argTypes = new int[]{Types.VARCHAR};
		int count = this.jdbcTemplate.queryForObject(SQL_USERS_COUNT, args, argTypes,Integer.class);
		return count > 0 ? true : false;
	}

	public JdbcTemplate getJdbcTemplate() {
		return jdbcTemplate;
	}

	public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}
	
	
}
