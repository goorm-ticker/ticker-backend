package com.goorm.ticker.fixture;

import com.goorm.ticker.user.entity.User;

public enum UserFixture {
	USER_FIXTURE_1(
		"김민수",
		"minsu01",
		"password123"
	),
	USER_FIXTURE_2(
		"박지연",
		"jiyeon02",
		"password456"
	),
	USER_FIXTURE_3(
		"이영희",
		"younghee03",
		"password789"
	),
	USER_FIXTURE_4(
		"최준혁",
		"junhyuk04",
		"password1234"
	),
	USER_FIXTURE_5(
		"정은지",
		"eunji05",
		"asdfgh5678"
	);

	private final String name;
	private final String loginId;
	private final String password;

	UserFixture(String name, String loginId, String password) {
		this.name = name;
		this.loginId = loginId;
		this.password = password;
	}

	public User createUser() {
		return User.builder()
			.name(name)
			.loginId(loginId)
			.password(password)
			.build();
	}
}
