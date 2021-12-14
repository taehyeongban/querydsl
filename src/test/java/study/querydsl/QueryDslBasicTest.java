package study.querydsl;

import com.querydsl.core.QueryResults;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMember.member;

@SpringBootTest
@Transactional
public class QueryDslBasicTest {

    @Autowired
    EntityManager em;
    JPAQueryFactory qf;

    @BeforeEach
    public void before() {
        qf = new JPAQueryFactory(em);
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");

        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);

        em.flush();
        em.clear();
    }

    @Test
    public void startJPQL() {
        Member findMember = (Member) em.createQuery("select m from Member m where m.username = :username")
                .setParameter("username", "member1")
                .getSingleResult();
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void startQueryDSL() {
        // QMember m1 = new QMember("m1"); 같은 테이블을 조인해야 하는 경우 선언해서 사용

        Member findMember = qf
                .select(member)
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();
        assertThat(findMember.getUsername()).isEqualTo("member1");

    }

    @Test
    public void search() {
        Member member1 = qf.selectFrom(member).where(member.username.eq("member1").and(member.age.eq(10))).fetchOne();
        assertThat(member1.getUsername()).isEqualTo("member1");
    }

    @Test
    public void searchAndParam() {
        Member member1 = qf.selectFrom(member).where(member.username.eq("member1"), member.age.eq(10)).fetchOne();
        assertThat(member1.getUsername()).isEqualTo("member1");
    }

    @Test
    public void resultFetch() {
        List<Member> fetch = qf.selectFrom(member).fetch();

        // Member fetchOne = qf.selectFrom(member).fetchOne(); // 결과가 없으면 null, 1개보다 많으면 Error

        Member fetchFirst = qf.selectFrom(member).fetchFirst();

        QueryResults<Member> memberQueryResults = qf.selectFrom(member).fetchResults(); // query 2번 실행됨, paging
        memberQueryResults.getTotal();
        List<Member> members = memberQueryResults.getResults();

        long total = qf.selectFrom(member).fetchCount(); // count 조회
    }

    @Test
    public void sort() {
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));
        List<Member> members = qf.selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();
        assertThat(members.size()).isEqualTo(3);
        assertThat(members.get(0).getUsername()).isEqualTo("member5");
        assertThat(members.get(1).getUsername()).isEqualTo("member6");
        assertThat(members.get(2).getUsername()).isEqualTo(null);
    }
}
