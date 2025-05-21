package com.learn.schedule;

import com.alibaba.schedulerx.worker.domain.JobContext;
import com.alibaba.schedulerx.worker.processor.JavaProcessor;
import com.alibaba.schedulerx.worker.processor.ProcessResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Optional;

/**
 * @author yujintao
 * @date 2025/5/16
 */
@Slf4j
public abstract class BaseJavaProcessor extends JavaProcessor {
    @Override
    public ProcessResult process(JobContext context) {
        long start = System.currentTimeMillis();

        String jobName = setJobName(context);
        String jobId = getJobId(context);
        String jobParams = getJobParams(context);
        try {
            execute(context);
            String format = String.format("jobId:%s,jobName:%s,result:success,duration:%s ms,jobParams:%s", jobId, jobName, System.currentTimeMillis() - start, jobParams);
            log.info(format);
            return new ProcessResult(true);
        } catch (Exception e) {
            String format = String.format("jobId:%s,jobName:%s,result:error,duration:%s ms,jobParams:%s", jobId, jobName, System.currentTimeMillis() - start, jobParams);
            log.error(format, e);
            return new ProcessResult(false, e.getMessage());
        }
    }

    /**
     * 任务ID：可以理解为调用链ID，用于排查问题
     *
     * @return
     */
    public String getJobId(JobContext context) {
        return context.getUniqueId();
    }

    /**
     * 任务参数
     *
     * @return
     */
    public String getJobParams(JobContext context) {
        String jobParameters = context.getJobParameters();
        return Optional.ofNullable(jobParameters).map(StringUtils::trim).orElse("");
    }

    /**
     * 任务名称
     *
     * @param context
     * @return
     */
    public abstract String setJobName(JobContext context);

    /**
     * 任务处理
     *
     * @param context
     * @throws Exception
     */
    public abstract void execute(JobContext context) throws Exception;
}
