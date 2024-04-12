package com.study.querydsl;


import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.study.querydsl.entity.Member;
import com.study.querydsl.entity.QMember;
import com.study.querydsl.entity.QTeam;
import com.study.querydsl.entity.Team;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;
import org.assertj.core.api.Assert;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.study.querydsl.entity.QMember.member;
import static com.study.querydsl.entity.QTeam.team;

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
                .limit(1)
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

    @Test
    public void paging1(){
        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)  // 0부터 시작(zero index)
                .limit(2)   // 최대 2건 조회
                .fetch(); // 리스트로 조회

        Assertions.assertThat(result.size()).isEqualTo(2);
    }

    @Test
    public void paging2(){
        QueryResults<Member> queryResults = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetchResults();    // 총 건수 + 리스트

        Assertions.assertThat(queryResults.getTotal()).isEqualTo(4);
        Assertions.assertThat(queryResults.getLimit()).isEqualTo(2);
        Assertions.assertThat(queryResults.getOffset()).isEqualTo(1);
        Assertions.assertThat(queryResults.getResults().size()).isEqualTo(2);

    }

    @Test
    public void aggregation() throws Exception {
        // 집합 함수
        // querydsl 에서 제공하는 Tuple

        List<Tuple> result = queryFactory
                .select(member.count(),  // 총 수
                        member.age.sum(),       // 합계
                        member.age.avg(),       // 평균
                        member.age.max(),       // 최대값
                        member.age.min())       // 최저값
                .from(member)
                .fetch();

        Tuple tuple = result.get(0);

        Assertions.assertThat(tuple.get(member.count())).isEqualTo(4);
        Assertions.assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        Assertions.assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        Assertions.assertThat(tuple.get(member.age.max())).isEqualTo(40);
        Assertions.assertThat(tuple.get(member.age.min())).isEqualTo(10);

    }

    @Test
    public void group() throws Exception {
        List<Tuple> result = queryFactory
                .select(team.name, member.age.avg())   // static import
                .from(member)
                .join(member.team, team)        // member tabel, team table join
                .groupBy(team.name)
                //.having()                     // 그룹화 조건문
                .fetch();

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        Assertions.assertThat(teamA.get(team.name)).isEqualTo("teamA");
        Assertions.assertThat(teamA.get(member.age.avg())).isEqualTo(15);

        Assertions.assertThat(teamB.get(team.name)).isEqualTo("teamB");
        Assertions.assertThat(teamB.get(member.age.avg())).isEqualTo(15);
    }


    /**
     * join(조인 대상, 별칭으로 사용할 Q타입)
     */
    @Test
    public void join() {

        List<Member> result = queryFactory
                .selectFrom(member)
                .join(member.team, team)
                //.innerJoin(member.team, team)
                //.leftJoin(member.team, team)
                //.rightJoin(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        Assertions.assertThat(result)
                .extracting("useername")
                .containsExactly("member1", "member2");
    }

    /**
     * 세타 조인 (연관 관계 없이 필드로 조인)
     * 아우터 조인 불가하지만 조인 on 절을 사용하여 외부 조인 가능
     */
    @Test
    public void theta_join() throws Exception{
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        List<Member> result = queryFactory
                .select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();

        Assertions.assertThat(result)
                .extracting("username")
                .containsExactly("teamA", "teamB");
    }


    /**
     * on 절 조인
     * 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회
     * JPQL :  select m, t from Member m left join m.join m.team, t on t.name = 'teamA'
     */
    @Test
    public void join_on_filtering() {
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team).on(team.name.eq("teamA"))        // 아우터 조인 시 on절로
                //.join(member.team, team).where(team.name.eq("teamA"))     // 이너 조인 시 where절로
                .fetch();

        for (Tuple tuple : result){
            System.out.println("tuple = "+tuple);
        }
    }

    /**
     * 연관관계 없는 엔티티 외부 조인
     */
    @Test
    public void join_on_no_relation() {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team)
                .on(member.username.eq(team.name))
                .fetch();

        Assertions.assertThat(result)
                .extracting("username")
                .containsExactly("teamA", "teamB");

    }

    /**
     * 페치 조인
     * SQL조인을 활용해서 연관된 엔티티를 한번에 조회하는 기능으로 성능 최적화에 사용하는 방법
     */
    @PersistenceUnit
    EntityManagerFactory emf;

    @Test
    public void fetchJoinNo(){

        // 영속성 컨텍스트를 먼저 초기화 해줘야함 결과를 제대로 볼 수 없기 때문
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .join(member.team, team)
                .fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();

        // 페치 조인 적용 여부를 알 수 있다.
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        Assertions.assertThat(loaded).as("페치 조인 미적용").isFalse();
    }


    /**
     * JPAExpressions 사용
     */
    @Test
    public void subQuery() throws Exception {

        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        JPAExpressions
                                .select(memberSub.age.max())
                                .from(memberSub)
                ))
                .fetch();

        Assertions.assertThat(result).extracting("age")
                .containsExactly(40);
    }

    @Test
    public void selectSubQuery(){

        QMember memberSub = new QMember("memberSub");

        List<Tuple> result = queryFactory
                .select(member.username,
                        JPAExpressions
                                .select(memberSub.age.avg())
                                .from(memberSub))
                .from(member)
                .fetch();

        for (Tuple tuple : result){
            System.out.println("tuple=" +tuple);
        }
    }


    /**
     * Case 문
     * .when() .then()
     * select, where문에서 사용 가능
     */
    @Test
    public void basicCase(){
        List<String> result = queryFactory
                .select(member.age
                        .when(10).then("열살")
                        .when(20).then("스무살")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = "+s);
        }

    }

    /**
     * 상수
     * Expressions.constant()
     */
    @Test
    public void constant() {
        List<Tuple> result = queryFactory
                .select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();

        for (Tuple tuple : result){
            System.out.println("tuple ="+tuple);
        }
    }

    /**
     * 문자 더하기
     * concat
     * 문자 타입으로 변환(특히 enum 사용 시 유용)  .stringValue()
     */
    @Test
    public void concat(){
        List<String> result = queryFactory
                .select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .where(member.username.eq("member1"))
                .fetch();

        for (String s : result){
            System.out.println("s =" +s);
        }

    }



}
