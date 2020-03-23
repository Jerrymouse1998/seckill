package web;

import dto.Exposer;
import dto.SeckillExecution;
import dto.SeckillResult;
import enums.SeckillState;
import exception.RepeatKillException;
import exception.SeckillCloseException;
import exception.SeckillException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import pojo.Seckill;
import service.SeckillService;

import java.util.Date;
import java.util.List;

@Controller
@RequestMapping(value = "/seckill")//URL:/模块
public class SeckillController {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private SeckillService seckillService;

    @RequestMapping(value = "/list",
                    method = RequestMethod.GET)
    public String list(Model model) {
        List<Seckill> list = seckillService.getSeckillList();
        model.addAttribute("list", list);
        return "list";
    }

    @RequestMapping(value = "/{seckillId}/detail",
                    method = RequestMethod.GET)
    public String detail(@PathVariable("seckillId") Long seckillId, Model model) {
        if (seckillId == null) {
            return "redirect:/seckill/list";
        }
        Seckill seckill = seckillService.getById(seckillId);
        if (seckill == null) {
            return "forward:/seckill/list";
        }
        model.addAttribute("seckill", seckill);
        return "detail";
    }

    //ajax接口直接返回json
    @RequestMapping(value = "/{seckillId}/exposer",
                    method = RequestMethod.POST,
                    produces = "application/json;charset=UTF-8")
    @ResponseBody
    public SeckillResult<Exposer> exposer(@PathVariable("seckillId")Long seckillId) {
        SeckillResult<Exposer> seckillResult;
        try {
            Exposer exposer = seckillService.exposerSeckillUrl(seckillId);
            seckillResult = new SeckillResult<Exposer>(true, exposer);
        } catch (Exception e) {
            logger.error(e.getMessage());
            seckillResult = new SeckillResult<>(false, e.getMessage());
        }
        return seckillResult;
    }

    @RequestMapping(value = "/{seckillId}/{md5}/execution",
                    method = RequestMethod.POST,
                    produces = "application/json;charset=UTF-8")
    @ResponseBody
    //因为暂时没有登录模块，就直接从cookie中获取userPhone作为凭证
    public SeckillResult<SeckillExecution> execute(@PathVariable("seckillId")Long seckillId,
                                                   @CookieValue(value = "killPhone",required = false)Long phone,
                                                   @PathVariable("md5")String md5){
        if (phone==null){
            return new SeckillResult<>(false,"没有已登录的手机号");
        }
        try {
            SeckillExecution execution = seckillService.executeSeckillProcedure(seckillId, phone, md5);
            return new SeckillResult<>(true,execution);
        } catch (SeckillCloseException sce) {
            SeckillExecution execution=new SeckillExecution(seckillId, SeckillState.END);
            return new SeckillResult<>(true,execution);
        } catch (RepeatKillException rke) {
            SeckillExecution execution=new SeckillExecution(seckillId, SeckillState.REPEAT_KILL);
            return new SeckillResult<>(true,execution);
        }catch (SeckillException e) {
            SeckillExecution execution=new SeckillExecution(seckillId, SeckillState.INNER_ERROR);
            return new SeckillResult<>(true,execution);
        }
    }

    @RequestMapping(value = "/time/now",
                    method = RequestMethod.GET)
    public @ResponseBody SeckillResult<Long> time(){
        logger.info("URL:/seckill/time/now;");
        Date date=new Date();
        return new SeckillResult<>(true,date.getTime());
    }
}

