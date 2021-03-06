/*
 * Copyright 2012-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.health;

import java.util.Properties;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Provider;
import javax.mail.Provider.Type;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.URLName;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link MailHealthIndicator}.
 *
 * @author Johannes Stelzer
 */
public class MailHealthIndicatorTests {

	private JavaMailSenderImpl mailSender;
	private MailHealthIndicator indicator;

	@Before
	public void setup() {
		Session session = Session.getDefaultInstance(new Properties());
		session.addProvider(new Provider(Type.TRANSPORT, "success",
				SuccessTransport.class.getName(), "Test", "1.0.0"));

		session.addProvider(new Provider(Type.TRANSPORT, "fail", FailTransport.class
				.getName(), "Test", "1.0.0"));

		session.addProvider(new Provider(Type.TRANSPORT, "failOnClose",
				FailOnCloseTransport.class.getName(), "Test", "1.0.0"));

		this.mailSender = mock(JavaMailSenderImpl.class);
		when(this.mailSender.getHost()).thenReturn("smtp.acme.org");
		when(this.mailSender.getPort()).thenReturn(25);
		when(this.mailSender.getSession()).thenReturn(session);

		this.indicator = new MailHealthIndicator(this.mailSender);
	}

	@Test
	public void smtpIsUp() {
		when(this.mailSender.getProtocol()).thenReturn("success");

		Health health = this.indicator.health();

		assertEquals(Status.UP, health.getStatus());
		assertEquals("smtp.acme.org:25", health.getDetails().get("location"));
	}

	@Test
	public void smtpIsDown() {
		when(this.mailSender.getProtocol()).thenReturn("fail");

		Health health = this.indicator.health();

		assertEquals(Status.DOWN, health.getStatus());
		assertEquals("smtp.acme.org:25", health.getDetails().get("location"));
	}

	@Test
	public void unexpectedExceptionOnClose() {
		when(this.mailSender.getProtocol()).thenReturn("failOnClose");

		Health health = this.indicator.health();

		assertEquals(Status.DOWN, health.getStatus());
		assertEquals("smtp.acme.org:25", health.getDetails().get("location"));
	}

	public static class SuccessTransport extends Transport {
		public SuccessTransport(Session session, URLName urlname) {
			super(session, urlname);
		}

		@Override
		public synchronized void connect(String host, int port, String user,
				String password) throws MessagingException {
		}

		@Override
		public void sendMessage(Message msg, Address[] addresses)
				throws MessagingException {
		}

	}

	public static class FailTransport extends SuccessTransport {
		public FailTransport(Session session, URLName urlname) {
			super(session, urlname);
		}

		@Override
		public synchronized void connect(String host, int port, String user,
				String password) throws MessagingException {
			throw new MessagingException("fail on connect");
		}
	}

	public static class FailOnCloseTransport extends SuccessTransport {
		public FailOnCloseTransport(Session session, URLName urlname) {
			super(session, urlname);
		}

		@Override
		public synchronized void close() throws MessagingException {
			throw new MessagingException("fail on close");
		}
	}

}
