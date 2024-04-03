package com.study.querydsl.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@Commit
class MemberTest {

    @Autowired
    EntityManager em;

    @Test
    public  void testEntity(){
        Team teamA = new Team("member1");
        Team teamB = new Team("member2");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);

        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4 ", 40, teamB);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);

        // 초기화
        em.flush();
        em.clear();

        List<Member> members  = em.createQuery("select m from Member m", Member.class).getResultList();


        // 확인
       /* Assertions.assertThat(members.get(1)).isEqualTo(member1);
        Assertions.assertThat(members.get(2)).isEqualTo(member2);
        Assertions.assertThat(members.get(3)).isEqualTo(member3);
        Assertions.assertThat(members.get(4)).isEqualTo(member4);*/

    }


}