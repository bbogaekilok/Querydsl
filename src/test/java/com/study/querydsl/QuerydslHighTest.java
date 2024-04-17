package com.study.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.study.querydsl.dto.MemberDto;
import com.study.querydsl.dto.QMemberDto;
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

import static com.study.querydsl.entity.QMember.*;

@SpringBootTest
@Transactional
public class QuerydslHighTest {

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


    /**
     * 프로젝션 : select 대상 지정
     * 프로젝션 대상이 하나일 경우
     */
    @Test
    public void simpleProjection() {
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s ="+s);
        }
    }

    /**
     * 프로젝션 대상이 둘 이상일 경우 Tuple을 사용한다.
     */
    @Test
    public void tupleProjection() {
        List<Tuple> result = queryFactory
                .select(member.username, member.age)
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            System.out.println("username : " + username);
            System.out.println("age : " + age);
        }
    }


    /**
     * 프로젝션 결과를 Dto로 반환하기
     * 순수 JPA 에서 Dto를 조회할 때 new 명령어를 사용함.
     * 경로를 다 적어줘야한다는 단점이 있으며
     * 생성자 방식만을 지원한다.
     */
    @Test
    public void findDtoByJPQL() {
        List<MemberDto> result = em.createQuery(
                "select new study.querydsl.dto.MemberDto(m.username, m.age) from Member m", MemberDto.class)
                .getResultList();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto :" + memberDto);
        }
    }

    /**
     * Querydsl로 Dto로 결과 반환하기 3가지 방법
     * 1. 프로퍼티 접근 - setter
     * Projections.bean() 를 활용
     */
    @Test
    public void findDtoBySetter(){
        List<MemberDto> result = queryFactory
                .select(Projections.bean(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = "+memberDto);
        }
    }


    /**
     * Querydsl로 Dto로 결과 반환하기 3가지 방법
     * 2. 필드 직접 접근
     * Projections.fields()
     */
    @Test
    public void findDtoByField(){
        List<MemberDto> result = queryFactory
                .select(Projections.fields(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = "+memberDto);
        }
    }

    /**
     * Querydsl로 Dto로 결과 반환하기 3가지 방법
     * 2. 필드 직접 접근
     * 별치이 다를 때 .as() 사용
     */
    @Test
    public void findUserDto(){
        QMember memberSub = new QMember("memberSub");

        List<MemberDto> result = queryFactory
                .select(Projections.fields(MemberDto.class,
                        member.username.as("name"), //이름이 안 맞을 땐 as를 이용하여 이름을 맞춰준다. 이름이 안 맞으면 매핑이 안됨.

                        // 서브쿼리
                        ExpressionUtils.as(JPAExpressions
                                .select(memberSub.age.max())
                                .from(memberSub), "age")
                ))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = "+memberDto);
        }
    }


    /**
     * Querydsl로 Dto로 결과 반환하기 3가지 방법
     * 3. 생성자 사용
     * Projections.constructor()
     */
    @Test
    public void findDtoByContructor(){
        List<MemberDto> result = queryFactory
                .select(Projections.constructor(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = "+memberDto);
        }
    }

    /**
     * @QueryProjection
     * Dto를 Q파일로 생성 함.
     */
    @Test
    public void findDtoByQueryProjection() {
        List<MemberDto> result = queryFactory
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = "+memberDto);
        }
    }

    /**
     * 동적 쿼리 해결하는 방법 1.booleanBuilder
     * where에서 다중 파라미터를 사용할 수 있다.
     */
    @Test
    public void dynamicQuery_BooleanBuilder() {
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> result = searchMember1(usernameParam, ageParam);
        Assertions.assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember1(String usernameCond, Integer ageCond) {

        BooleanBuilder builder = new BooleanBuilder();
        if(usernameCond != null) {
            builder.and(member.username.eq(usernameCond));
        }

        if(ageCond != null) {
            builder.and(member.age.eq(ageCond));
        }

        return queryFactory
                .selectFrom(member)
                .where(builder)
                .fetch();
    }

    /**
     * 동적 쿼리 해결하는 방법 2.Where 다중 파라미터 사용
     * 메서드를 다른 쿼리에서 재활용 할 수 있다.
     * 메서드를 조합할 수 있다.
     * where 조건에 null 값은 무시된다.
     * 메서드에서 null 체크 해줘야함.
     */
    @Test
    public void dynamicQuery_WhereParam() throws Exception {
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> result = searchMember2(usernameParam, ageParam);
        Assertions.assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember2(String usernameCond, Integer ageCond) {
        return  queryFactory
                .selectFrom(member)
                .where(allEq(usernameCond,ageCond))
                .fetch();
    }

    private BooleanExpression usernameEq(String usernameCond) {
        return usernameCond != null ? member.username.eq(usernameCond) : null ;
    }

    private BooleanExpression ageEq(Integer ageCond) {
        return ageCond != null ? member.age.eq(ageCond) : null ;
    }

    private BooleanExpression allEq(String usernameCond, Integer ageCond){
        return usernameEq(usernameCond).and(ageEq(ageCond));
    }



}
