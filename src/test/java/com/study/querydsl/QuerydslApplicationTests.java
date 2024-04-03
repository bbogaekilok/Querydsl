package com.study.querydsl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.study.querydsl.entity.Hello;
import com.study.querydsl.entity.QHello;
import jakarta.persistence.EntityManager;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class QuerydslApplicationTests {

	@Autowired
	EntityManager em;

	@Test
	void contextLoads() {
		Hello hello = new Hello();
		em.persist(hello);

		JPAQueryFactory jpaQueryFactory = new JPAQueryFactory(em);
		QHello qHello = new QHello("h");

		Hello result = jpaQueryFactory.selectFrom(qHello).fetchOne();

		Assertions.assertThat(result).isEqualTo(hello);
		Assertions.assertThat(result.getId()).isEqualTo(hello.getId());

	}

}
