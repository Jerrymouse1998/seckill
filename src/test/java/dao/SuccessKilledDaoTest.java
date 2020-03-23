package dao;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:spring/spring-dao.xml"})
public class SuccessKilledDaoTest {

    @Autowired
    SuccessKilledDao successKilledDao;

    @Test
    public void insertSuccessKilled() {
        int i=successKilledDao.insertSuccessKilled(1001,18658964875l);
        System.out.println(i);
    }

    @Test
    public void queryByIdWihtSeckill() {
        System.out.println(successKilledDao.queryByIdWihtSeckill(1000,18658964875l));
    }
}