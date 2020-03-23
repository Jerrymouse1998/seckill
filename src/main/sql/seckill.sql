DELIMITER $$ -- console ; 转换成 $$
-- 定义存储过程
-- row_count():返回上一条修改型sql的结果到变量中，<0 sql错误或未执行，==0 未修改数据
-- 参数：in 输入参数，out 输出参数
CREATE PROCEDURE `seckill`.`execute_seckill`
        (in v_seckill_id bigtint,in v_phone bigint,in v_kill_time timestamp
        ,out r_result int)
    BEGIN
    DECLARE insert_count int DEFAULT 0;-- 定义变量
    START TRANSACTION;-- 开启事务
    insert ignore into success_killed
        (seckill_id,user_phone,create_time)
        values(v_seckill_id,v_phone,v_kill_time);
    select row_count() into insert_count;
    IF(insert_count = 0) then
        ROLLBACK;
        set r_result = -1; -- 对应数据字典中的 重复秒杀"REPEAT_KILL"
    ELSEIF(insert_count < 0) then
        ROLLBACK ;
        set r_result = -2; -- 对应数据字典中的 系统异常"INNER_ERROR"
    ELSE
        update seckill
            set number = number-1
            where  seckillId = seckill_id
            and v_kill_time < end_time
            and v_kill_time > start_time
            and number > 0;
        select row_count() into insert_count;
        IF(insert_count = 0) then
            ROLLBACK;
            set r_result = 0; -- 秒杀结束"END"
        ELSEIF(insert_count < 0) then
            ROLLBACK ;
            set r_result = -2; -- 系统异常"INNER_ERROR"
        ELSE
            COMMIT;
            set r_result = 1; -- 秒杀成功"SUCCESS"
        END IF;
    END IF;
    END;
$$
--存储过程定义结束
