package dao;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Date;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:spring/spring-dao.xml"})
public class SeckillDaoTest {

    @Autowired
    SeckillDao seckillDao;

    @Test
    public void reduceNumber() {
        Date date=new Date();
        int i = seckillDao.reduceNumber(1003l, date);
        System.out.println(i);
    }

    @Test
    public void queryById() {
        System.out.println(seckillDao.queryById(1002l));
    }

    @Test
    public void queryAll() {
        System.out.println(seckillDao.queryAll(0,5));
    }
}