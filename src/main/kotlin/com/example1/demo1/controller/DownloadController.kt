package com.example1.demo1.controller

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.onClosed
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.apache.commons.io.IOUtils
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.InputStreamResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import java.io.*
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import kotlin.system.*
import javax.servlet.http.HttpServletResponse
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.*


@RestController
class DownloadController {

    val header = HttpHeaders().also{
        it.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=temp-${System.currentTimeMillis()}.txt")
        it.add("Cache-Control", "no-cache, no-store, must-revalidate")
        it.add("Pragma", "no-cache")
        it.add("Expires", "0")
    }

    @RequestMapping(path = ["/download1"], method = [RequestMethod.GET]) //WORKS
    @Throws(IOException::class)
    fun download1(response: HttpServletResponse) {

        val timeInMillis = measureTimeMillis {
            response.addHeader("Content-Disposition", "attachment; filename=temp-${System.currentTimeMillis()}.txt")
            response.addHeader("Cache-Control", "no-cache, no-store, must-revalidate")
            response.addHeader("Pragma", "no-cache")
            response.addHeader("Expires", "0")
            response.contentType = "application/octet-stream"
            for (it in 0..20000000) { // do not use forEach{}, since u reach the response.flushBuffer() line, before loop ended.
                IOUtils.copy("bla-$it\n".byteInputStream(), response.outputStream)
//            println(">> $it")
            }
            response.flushBuffer()
        }

        println(">>> elapsedTime=$timeInMillis")
    }

    @RequestMapping(path = ["/download2"], method = [RequestMethod.GET])
    @Throws(IOException::class)
    fun download2(): ResponseEntity<StreamingResponseBody> {
        val stream = StreamingResponseBody { out: OutputStream? ->
            (0..20000000).forEach { IOUtils.copy("bla-$it\n".byteInputStream(), out) }
        }

        return ResponseEntity.ok()
            .headers(header)
            .contentType(MediaType.parseMediaType("application/octet-stream"))
            .body(stream)
    }

    @RequestMapping(path = ["/download3"], method = [RequestMethod.GET])
    @Throws(IOException::class)
    fun download3(): ResponseEntity<StreamingResponseBody> {
        val stream = StreamingResponseBody { out: OutputStream? ->
            val dp = dataProducer()
            runBlocking {
                dp.collect {
                    IOUtils.copy(it, out)
                }
            }
        }

        return ResponseEntity.ok()
            .headers(header)
            .contentType(MediaType.parseMediaType("application/octet-stream"))
            .body(stream)
    }
}

fun dataProducer() : Flow<ByteArrayInputStream> = flow {
    (0..20000000).forEach{
        println(">>> $it")
        emit("bla-$it\n".byteInputStream())}
}