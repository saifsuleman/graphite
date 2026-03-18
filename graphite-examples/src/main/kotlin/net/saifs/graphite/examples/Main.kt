package net.saifs.graphite.examples

import net.saifs.graphite.di.Injector
import net.saifs.graphite.di.annotation.Named
import net.saifs.graphite.scanning.ClasspathScanningModule

fun main() {
    val module = ClasspathScanningModule("net.saifs")

    val root = Injector.create {
        bind<PrintServiceImpl>()
        bind<MessageServiceImpl>()
    }

    val child = root.child {
        bind("message", "OOGOGA BOGOOA")
    }

    val print = child.inject<PrintService>()
    print.print()
}

interface PrintService {
    fun print()
}

interface MessageService {
    fun message(): String
}

class PrintServiceImpl(private val messageService: MessageService) : PrintService {
    override fun print() {
        println(messageService.message())
    }
}

class MessageServiceImpl(
    @Named("message")
    private val message: Lazy<String>
) : MessageService {
    override fun message(): String = message.value
}