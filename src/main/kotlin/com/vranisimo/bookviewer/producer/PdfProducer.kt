package com.vranisimo.bookviewer.producer

import com.google.gson.GsonBuilder
import com.vranisimo.bookviewer.model.ProcessPdfMessage
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import org.springframework.stereotype.Component
import org.springframework.util.concurrent.ListenableFuture

@Component
class PdfProducer {
    private val logger: Logger = LoggerFactory.getLogger(this.javaClass)

    @Autowired
    constructor(kafkaTemplate: KafkaTemplate<String, String>) {
        this.kafkaTemplate = kafkaTemplate
    }

    var kafkaTemplate: KafkaTemplate<String, String>? = null;
    val topic: String = "extract-pdf-page"

    fun sendMessage(message: ProcessPdfMessage) {
        val messageInString: String = GsonBuilder().setPrettyPrinting().create().toJson(message)
        val lf: ListenableFuture<SendResult<String, String>> = kafkaTemplate?.send(topic, messageInString)!!
        val sendResult: SendResult<String, String> = lf.get()

        logger.info("Message is sent to topic:\n" + sendResult.producerRecord.value())
    }
}