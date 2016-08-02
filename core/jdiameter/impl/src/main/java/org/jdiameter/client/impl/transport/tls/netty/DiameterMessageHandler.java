/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2016, TeleStax Inc. and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package org.jdiameter.client.impl.transport.tls.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

import org.jdiameter.api.Avp;
import org.jdiameter.api.AvpDataException;
import org.jdiameter.client.api.IMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author <a href="mailto:jqayyum@gmail.com"> Jehanzeb Qayyum </a>
 */
public class DiameterMessageHandler extends ChannelInboundHandlerAdapter {
  private static final Logger logger = LoggerFactory.getLogger(DiameterMessageHandler.class);

  private final TLSClientConnection parentConnection;

  public DiameterMessageHandler(TLSClientConnection parentConnection) {
    this.parentConnection = parentConnection;
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    if (msg instanceof IMessage) {
      IMessage m = (IMessage) msg;
      logger.debug("Received message {} TLS Transport {}", m.getCommandCode(), this.parentConnection.getKey());
      try {
        logger.debug("Passing message on to parent {}", this.parentConnection.getKey());
        parentConnection.onMessageReceived(m);
        logger.debug("Finished passing message on to parent {}", this.parentConnection.getKey());

        // sslHandler setup
        Avp inbandAvp = m.getAvps().getAvp(Avp.INBAND_SECURITY_ID);
        boolean hasInbandSecurity = inbandAvp != null && inbandAvp.getUnsigned32() == 1;
        logger.debug("hasInbandSecurity {}", hasInbandSecurity);
        if (m.getCommandCode() == IMessage.CAPABILITIES_EXCHANGE_REQUEST) {
          parentConnection.getClient().setHasClientInbandSecurity(hasInbandSecurity);
        } else if (m.getCommandCode() == IMessage.CAPABILITIES_EXCHANGE_ANSWER && hasInbandSecurity) {
          parentConnection.getClient().installSslHandler(ctx.channel());
        }

      } catch (AvpDataException e) {
        logger.debug("Garbage was received. Discarding. {}", this.parentConnection.getKey());
        parentConnection.onAvpDataException(e);
      } finally {
        ReferenceCountUtil.release(msg);
      }
    }
  }

}
