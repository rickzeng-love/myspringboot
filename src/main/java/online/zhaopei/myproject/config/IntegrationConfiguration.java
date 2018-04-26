package online.zhaopei.myproject.config;

import online.zhaopei.myproject.integration.DefaultFileNameGenerator;
import online.zhaopei.myproject.integration.QueueReadingMessageSource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.*;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.file.FileNameGenerator;
import org.springframework.integration.file.FileReadingMessageSource;
import org.springframework.integration.file.FileWritingMessageHandler;
import org.springframework.integration.file.filters.SimplePatternFileListFilter;
import org.springframework.integration.file.transformer.FileToStringTransformer;
import org.springframework.integration.router.HeaderValueRouter;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.handler.annotation.Header;

import java.io.File;
import java.util.Map;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

import static org.springframework.integration.support.MutableMessageBuilder.withPayload;

/**
 * Created by zhaopei on 17/12/20.
 */
@Configuration
@EnableIntegration
public class IntegrationConfiguration {

    public static BlockingDeque<String[]> RECEIPT_QUEUE = new LinkedBlockingDeque<String[]>(1000);

    private static final Log logger = LogFactory.getLog(IntegrationConfiguration.class);

    @Bean
    public MessageChannel fileInputChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel fileProcessChannel() {
        return new PublishSubscribeChannel();
    }

    @Bean
    public MessageChannel fileOutputChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel receiptChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel receiptFileChannel() {
        return new DirectChannel();
    }

    @Bean
    @InboundChannelAdapter(value = "fileInputChannel", poller = @Poller(fixedDelay = "100", maxMessagesPerPoll = "5000"))
    public MessageSource<File> fileReadingMessageSource() {
        FileReadingMessageSource source = new FileReadingMessageSource();
        source.setDirectory(new File("/Users/zhaopei/work/test/send"));
        source.setFilter(new SimplePatternFileListFilter("*.xml"));
        return source;
    }

    @Bean
    @InboundChannelAdapter(value = "receiptChannel", poller = @Poller(fixedDelay = "100", maxMessagesPerPoll = "1000"))
    public MessageSource<String> queueReadingMessageSource() {
        return new QueueReadingMessageSource();
    }

    @Bean
    @ServiceActivator(inputChannel = "receiptFileChannel")
    public MessageHandler receiptFileWritingMessageHandler() {
        FileWritingMessageHandler handler = new FileWritingMessageHandler(new File("/Users/zhaopei/work/test/message"));
        handler.setDeleteSourceFiles(true);
        handler.setExpectReply(false); //没有replyChannel或者outputChannel要设置成false，不然会出错
        handler.setFileNameGenerator(new DefaultFileNameGenerator("FILE_RECEIVE_", ".xml", false));
        return handler;
    }

    @Bean
    @ServiceActivator(inputChannel = "fileOutputChannel")
    public MessageHandler fileWritingMessageHandler() {
        FileWritingMessageHandler handler = new FileWritingMessageHandler(new File("/Users/zhaopei/work/test/receive"));
        handler.setDeleteSourceFiles(true);
        handler.setExpectReply(false); //没有replyChannel或者outputChannel要设置成false，不然会出错
        handler.setFileNameGenerator(new DefaultFileNameGenerator("FILE_RECEIVE_", ".csv", false));
        return handler;
    }

    @Bean
    @ServiceActivator(inputChannel = "fileProcessChannel")
    public MessageHandler anotherFileWritingMessageHandler() {
        FileWritingMessageHandler handler = new FileWritingMessageHandler(new File("/Users/zhaopei/work/test/222"));
        handler.setDeleteSourceFiles(true);
        handler.setExpectReply(false); //没有replyChannel或者outputChannel要设置成false，不然会出错
        handler.setFileNameGenerator(new DefaultFileNameGenerator("FILE_RECEIVE_", ".xml", false));
        return handler;
    }

    //@Bean
    //@ServiceActivator(inputChannel = "receiptChannel")
    public HeaderValueRouter headerValueRouter() {
        HeaderValueRouter route = new HeaderValueRouter("type");
        route.setChannelMapping("r", "fileOutputChannel");
        route.setChannelMapping("m", "receiptFileChannel");
        route.setDefaultOutputChannelName("fileOutputChannel");
        return route;
    }

    /**
     * 第二种方式实现路由,用@Router注解
     * @param type
     * @return
     */
    @Router(inputChannel = "receiptChannel")
    public String typeRouter(@Header("type") String type) {
        return "r".equals(type) ? "fileOutputChannel" : "receiptFileChannel";
    }

    @ServiceActivator(inputChannel = "fileProcessChannel", outputChannel = "fileOutputChannel")
    public String processMessage(String message) {
        logger.info("processMessage=");
        //Message<String> outMsg = MessageBuilder.withPayload("处理消息 " + message).build();
        //fileOutputChannel().send(outMsg);
        return message;
    }

    @ServiceActivator(inputChannel = "fileProcessChannel")
    public void secondProcessMessage(String message, @Header("id") String headId) {
        logger.info("secondProcessMessage=" + headId);
    }

    @Bean
    @Transformer(inputChannel = "fileInputChannel", outputChannel = "fileProcessChannel")
    public FileToStringTransformer fileToStringTransformer() {
        FileToStringTransformer transformer = new FileToStringTransformer();
        transformer.setCharset("UTF-8");
        transformer.setDeleteFiles(true);
        return transformer;
    }
}
