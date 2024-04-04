package com.study.querydsl;


import com.querydsl.core.QueryResults;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.study.querydsl.entity.Member;
import com.study.querydsl.entity.QMember;
import com.study.querydsl.entity.Team;
import jakarta.persistence.EntityManager;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.study.querydsl.entity.QMember.member;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @Autowired
    EntityManager em;

    JPAQueryFactory queryFactory;

    @BeforeEach // 테스트 실행 전 작동
    public void before(){
        queryFactory = new JPAQueryFactory(em);
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
    }


    @Test
    public void startJPQL(){
        // JPQL 적용시
        // 오류 시점 (메소드 호출 시 오류가 남. 런타임 에러가 발생한다.)

        // 멤버 1 찾기
        Member findMember = em.createQuery("select m from Member m where m.username =: username", Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

        Assertions.assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void startQuerydsl(){
        // Querydsl 적용시
        // JDBC에 있는 PrepareStatement 로 파라미터 데이터 바인딩 자동처리가 가능하다.
        // 컴파일 시점에 오류를 알 수 있음

        // JPAQueryFactory queryFactory = new JPAQueryFactory(em);  // 필드 주입

        QMember m = new QMember("m");
        Member findMember = queryFactory
                .select(m)
                .from(m)
                .where(m.username.eq("member1"))
                .fetchOne();


        Assertions.assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void startQuerydsl2(){

        // 별칭 직접 지정 방법
        QMember m = new QMember("m");
        Member findMember = queryFactory
                .select(m)
                .from(m)
                .where(m.username.eq("member1"))
                .fetchOne();

        Assertions.assertThat(findMember.getUsername()).isEqualTo("member1");

    }

    @Test
    public void startQuerydsl3(){

        // 기본 인스턴스를 static import와 사용
        Member findMember = queryFactory
                .select(member) //QMember.member static import가능(opt+enter)
                .from(member)
                .where(member .username.eq("member1"))
                .fetchOne();

        Assertions.assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void startQuerydsl4(){
        // 같은 테이블을 조인 시 alias를 다르게 줘야하는 경우 적용
        QMember m1 = new QMember("m1");

        Member findMember = queryFactory
                .select(m1)
                .from(m1)
                .where(m1 .username.eq("member1"))
                .fetchOne();

        Assertions.assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void search(){
        // 검색 쿼리
        Member findMember = queryFactory
                .selectFrom(member) // select+from 합칠 수 있음
                .where(member.username.eq("member1")
                        .and(member.age.eq(10)))
                .fetchOne();

        Assertions.assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void searchAndParam(){
        // AND 조건일 경우
        // WHERE 문에서 AND 조건이 추가된다.
        List<Member> result = queryFactory.selectFrom(member)
                .where(member.username.eq("member1"), member.age.eq(10))
                .fetch();

        Assertions.assertThat(result.size()).isEqualTo(1);
    }

    @Test
    public void resultFetch(){

        // 리스트 조회
        List<Member> result = queryFactory
                .selectFrom(member)
                .fetch();

        // 단건 조회
        Member result2 = queryFactory
                .selectFrom(member)
                .fetchOne();

        // 처음 한 건 조회 (.limit(1).fetchOne() 과 같음)
        Member result3 = queryFactory
                .selectFrom(member)
                .fetchFirst();

        // 페이징 정보를 조회 ( total count, 리스트 조회)
        // 리스트 조회와 카운트 쿼리가 두번 나감
        // 복잡한 페이징일 경우엔 권장하지 않음. 쿼리를 따로 작성하길 권장
        QueryResults<Member> result4 = queryFactory
                .selectFrom(member)
                .fetchResults();

        // total count
        result4.getTotal();
        // 리스트 정보
        result4.getResults();

        // count 조회
        long count = queryFactory
                .selectFrom(member)
                .fetchCount();
    }

    /**
     * 회원 정렬 순서
     * 1. 회원 나이 내림차순(desc)
     * 2. 회원 이름 올림차순(asc)
     * 단 2에서 회원 이름이 없으면 마지막에 출력(nulls last)
     */
    @Test
    public void sort(){
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        List<Member> result = queryFactory.selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();


        // age 는 같으므로 내림차순의 영향이 없고
        // 이름으로 오름차순하고 null 값을 제일 마지막으로 정렬하면
        // 아래와 같은 정렬 순으로 데이터가 조회된다.
        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);

        Assertions.assertThat(member5.getUsername()).isEqualTo("member5");
        Assertions.assertThat(member6.getUsername()).isEqualTo("member6");
        Assertions.assertThat(memberNull.getUsername()).isNull();
    }
}
