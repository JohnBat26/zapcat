package org.kjkoster.zapcat.test;

/* This file is part of Zapcat.
 *
 * Zapcat is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * Zapcat is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * Zapcat. If not, see <http://www.gnu.org/licenses/>.
 */

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;

import org.junit.After;
import org.junit.Test;
import org.kjkoster.zapcat.Trapper;
import org.kjkoster.zapcat.zabbix.ZabbixTrapper;

/**
 * Test cases to try the trapper configuration.
 * 
 * @author Kees Jan Koster &lt;kjkoster@kjkoster.org&gt;
 */
public class ZabbixTrapperConfigurationTest {
    private int read = -1;

    private final byte[] buffer = new byte[1024];

    final Properties originalProperties = (Properties) System.getProperties()
            .clone();

    /**
     * Restore the system properties.
     * 
     * @throws Exception
     *             When the test failed.
     */
    @After
    public void tearDown() throws Exception {
        System.setProperties(originalProperties);
        assertNull(System.getProperty(ZabbixTrapper.SERVER_PROPERTY));
        assertNull(System.getProperty(ZabbixTrapper.PORT_PROPERTY));
        assertNull(System.getProperty(ZabbixTrapper.HOST_PROPERTY));
    }

    /**
     * Test starting and stopping a simple trapper.
     * 
     * @throws Exception
     *             When the test failed.
     */
    @Test
    public void testStartAndStop() throws Exception {
        final Thread server = startServer(ZabbixTrapper.DEFAULT_PORT);

        trapSomeData();

        server.interrupt();
        server.join();
    }

    /**
     * Test starting and stopping a trapper on a non-standard server config.
     * 
     * @throws Exception
     *             When the test failed.
     */
    @Test
    public void testSomeOtherPort() throws Exception {
        final int TEST_PORT = ZabbixTrapper.DEFAULT_PORT + 1;
        final Thread server = startServer(TEST_PORT);

        System.setProperty(ZabbixTrapper.PORT_PROPERTY, "" + TEST_PORT);
        assertEquals("" + TEST_PORT, System
                .getProperty(ZabbixTrapper.PORT_PROPERTY));

        trapSomeData();

        server.interrupt();
        server.join();
    }

    private void trapSomeData() throws Exception {
        final Trapper trapper = new ZabbixTrapper("localhost", "foo");
        trapper.send("bar", "baz");

        trapper.stop();

        // we compare byte-for-byte to avoid unicode issues...
        for (int i = 0; i < read; i++) {
            assertEquals(
                    (byte) "<req><host>Zm9v</host><key>YmFy</key><data>YmF6</data></req>                                                       "
                            .charAt(i), buffer[i]);
        }
    }

    private Thread startServer(final int port) {
        final Thread server = new Thread(new Runnable() {
            public void run() {

                ServerSocket serverSocket = null;
                Socket accepted = null;
                try {
                    serverSocket = new ServerSocket(port);
                    accepted = serverSocket.accept();
                    read = accepted.getInputStream().read(buffer);
                } catch (Exception e) {
                    e.printStackTrace();
                    fail();
                } finally {
                    try {
                        accepted.close();
                        serverSocket.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                        fail();
                    }
                }
            }
        });
        server.start();
        return server;
    }
}
