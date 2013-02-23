package metridoc.core.tools

import groovy.util.logging.Slf4j
import metridoc.utils.CamelUtils
import org.apache.camel.CamelContext
import org.apache.camel.ConsumerTemplate
import org.apache.camel.Exchange
import org.apache.camel.ProducerTemplate
import org.apache.camel.impl.DefaultCamelContext
import org.apache.camel.impl.JndiRegistry
import org.apache.camel.util.jndi.JndiContext
import org.codehaus.groovy.runtime.typehandling.GroovyCastException

/**
 * Each routing method creates a new CamelContext using the binding to fill its registry and shuts it down before
 * the method is called.  This ensures that messages don't leek in after the method is called.  Suppose you consume
 * consume a queue endpoint such as jms or seda, after the method ends more messages could leak in if the context is not
 * shut down
 */
@Slf4j
class CamelTool {
    Binding binding
    private static ThreadLocal<DefaultCamelContext> camelContext = new ThreadLocal<DefaultCamelContext>()
    private static ThreadLocal<ProducerTemplate> producerTemplate = new ThreadLocal<ProducerTemplate>()
    private static ThreadLocal<ConsumerTemplate> consumerTemplate = new ThreadLocal<ConsumerTemplate>()

    def bindBinding() {
        withCamelContext { DefaultCamelContext camelContext ->
            JndiRegistry registry = camelContext.registry.registry as JndiRegistry
            JndiContext jndiContext = registry.context as JndiContext
            if (binding) {
                binding.variables.each { String key, value ->
                    jndiContext.bind(key, value)
                }
            }
        }
    }

    def withCamelContext(Closure closure) {
        def context = camelContext.get()
        if (!context) {
            withNewCamelContext(closure)
        } else {
            closure.call(context)
        }
    }

    def withNewCamelContext(Closure closure) {
        camelContext.set(new DefaultCamelContext(new JndiContext()))
        def context = camelContext.get()
        producerTemplate.set(context.createProducerTemplate())
        consumerTemplate.set(context.createConsumerTemplate())
        bindBinding()
        try {
            closure.call(context)
        } finally {
            if (context) {
                try {
                    if (context.started) {
                        context.shutdown()
                    }
                } catch (Exception ex) {
                    log.error("unexpected exception occurred shutting down camel", ex)
                }
                camelContext.set(null)
                producerTemplate.set(null)
                consumerTemplate.set(null)
            }
        }
    }

    void consume(String endpoint, Closure closure) {
        def consumer = { ConsumerTemplate consumerTemplate ->
            return consumerTemplate.receive(endpoint)
        }
        consumeHelper(consumer, closure)
    }

    void consumeNoWait(String endpoint, Closure closure) {
        def consumer = { ConsumerTemplate consumerTemplate ->
            return consumerTemplate.receiveNoWait(endpoint)
        }
        consumeHelper(consumer, closure)
    }

    void consumeWait(String endpoint, long wait, Closure closure) {
        def consumer = { ConsumerTemplate consumerTemplate ->
            return consumerTemplate.receive(endpoint, wait)
        }
        consumeHelper(consumer, closure)
    }

    void consumeTillDone(String endpoint, long wait = 5000L, Closure closure) {
        boolean hasValue = true
        withCamelContext {
            while (hasValue) {
                def consumer = { ConsumerTemplate consumerTemplate ->
                    Exchange exchange = consumerTemplate.receive(endpoint, wait)
                    hasValue = exchange != null
                    return exchange
                }
                consumeHelper(consumer, false, closure)
            }
        }
    }

    public <T> T convertTo(Class<T> convertion, valueToConvert) {
        withCamelContext {CamelContext camelContext ->
            def converted = camelContext.typeConverter.convertTo(convertion, valueToConvert)
            if (converted == null) {
                try {
                    converted = valueToConvert.asType(convertion)
                } catch (GroovyCastException ex) {
                    //do nothing, no convertion available
                }
            }
            return converted
        }
    }

    private void consumeHelper(Closure consume, boolean processNull = true, Closure processBody) {
        withCamelContext { CamelContext context ->
            ConsumerTemplate consumerTemplate = consumerTemplate.get()
            Exchange body = null
            try {
                body = consume.call(consumerTemplate)
                def parameter = processBody.parameterTypes[0]

                if (parameter == Exchange && body != null) {
                    processBody.call(body)
                } else {
                    def messageBody = null
                    if (body) {
                        messageBody = body.in.getBody(parameter)
                        if (messageBody == null) {
                            messageBody = body.in.body.asType(parameter)
                        }
                    }
                    if (messageBody != null) {
                        processBody.call(messageBody)
                    } else if (processNull) {
                        processBody.call(null)
                    }
                }
            } catch (Exception e) {
                if (body) {
                    body.exception = e
                }
                throw e
            } finally {
                if (body) {
                    consumerTemplate.doneUoW(body)
                }
            }
        }
    }

    void send(String endpoint, body) {
        send(endpoint, body, [:])
    }

    void send(String endpoint, body, Map headers) {
        withCamelContext {
            producerTemplate.get().requestBodyAndHeaders(endpoint, body, headers)
        }
    }

    void asyncSend(String endpoint, body) {
        asyncSend(endpoint, body, [:])
    }

    void asyncSend(String endpoint, body, Map headers) {
        withCamelContext { CamelContext context ->
            producerTemplate.get().asyncRequestBodyAndHeaders(endpoint, body, headers)
        }
    }
}
