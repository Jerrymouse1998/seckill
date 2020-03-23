package service;

import dto.Exposer;
import dto.SeckillExecution;
import exception.RepeatKillException;
import exception.SeckillCloseException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import pojo.Seckill;

import java.util.List;
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:spring/spring-dao.xml",
                        "classpath:spring/spring-service.xml"})
public class SeckillServiceTest {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private SeckillService seckillService;

    @Test
    public void getSeckillList() {
        List<Seckill> seckillList = seckillService.getSeckillList();
        logger.info("list={}",seckillList);
    }

    @Test
    public void getById() {
        Seckill seckillServiceById = seckillService.getById(1000l);
        logger.info("list={}",seckillServiceById);
    }

    @Test
    public void exposerSeckillUrl() {
        Exposer exposer = seckillService.exposerSeckillUrl(1000l);
        logger.info("exposer={}",exposer);
    }//exposed=true, md5='6aa422635cf597f6277dd60acce25cd0', seckillId=1000

    @Test
    public void executeSeckill() {
        try {
            SeckillExecution seckillExecution = seckillService.executeSeckill(1000, 18666666666l, "6aa422635cf597f6277dd60acce25cd0");
        }catch (SeckillCloseException seckillCloseExcetion) {
            logger.error(seckillCloseExcetion.getMessage());
        } catch (RepeatKillException repeatKillException) {
            logger.error(repeatKillException.getMessage());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Test
    public void seckillLogic(){
        Exposer exposer = seckillService.exposerSeckillUrl(1000l);
        logger.info("exposer={}",exposer);
        if (exposer.isExposed()){
            try {
                SeckillExecution seckillExecution = seckillService.executeSeckill(1000, 18666666666l, "6aa422635cf597f6277dd60acce25cd0");
            }catch (SeckillCloseException seckillCloseExcetion) {
                logger.error(seckillCloseExcetion.getMessage());
            } catch (RepeatKillException repeatKillException) {
                logger.error(repeatKillException.getMessage());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }else {
            logger.warn("exposer={}",exposer);
        }
    }

    @Test
    public void executeSeckillProcedure(){
        long seckillId=1000;
        long userPhone=44444444444l;
        Exposer exposer = seckillService.exposerSeckillUrl(seckillId);
        if (exposer.isExposed()) {
            SeckillExecution se = seckillService.executeSeckillProcedure(seckillId, userPhone, exposer.getMd5());
            logger.info(se.getStateInfo());
            logger.info(se.toString());
        }
    }
}