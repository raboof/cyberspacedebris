package cyberspacedebris

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.filter.Filter
import ch.qos.logback.core.spi.FilterReply

class BindLoggingFilter extends Filter[ILoggingEvent] {
  override def decide(event: ILoggingEvent): FilterReply = {
    if (event.getMessage.contains("Bind failed for TCP channel")) FilterReply.DENY;
    else FilterReply.NEUTRAL;
  }
}
