/*
 * Copyright 2017 - 2018 Anton Tananaev (anton@traccar.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.traccar.protocol;

import io.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.DeviceSession;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.util.regex.Pattern;

public class EskyProtocolDecoder extends BaseProtocolDecoder {

    public EskyProtocolDecoder(EskyProtocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .expression("..;")                   // header
            .number("d+;")                       // index
            .number("(d+);")                     // imei
            .text("R;")                          // data type
            .number("(d+)[+;]")                  // satellites
            .number("(dd)(dd)(dd)")              // date
            .number("(dd)(dd)(dd)[+;]")          // time
            .number("(-?d+.d+)[+;]")             // latitude
            .number("(-?d+.d+)[+;]")             // longitude
            .number("(d+.d+)[+;]")               // speed
            .number("(d+)[+;]")                  // course
            .groupBegin()
            .text("0x").number("(d+)[+;]")       // input
            .number("(d+)[+;]")                  // message type
            .number("(d+)[+;]")                  // odometer
            .groupEnd("?")
            .number("(d+)")                      // voltage
            .any()
            .compile();

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        Parser parser = new Parser(PATTERN, (String) msg);
        if (!parser.matches()) {
            return null;
        }

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, parser.next());
        if (deviceSession == null) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        position.set(Position.KEY_SATELLITES, parser.nextInt());

        position.setValid(true);
        position.setTime(parser.nextDateTime());
        position.setLatitude(parser.nextDouble());
        position.setLongitude(parser.nextDouble());
        position.setSpeed(UnitsConverter.knotsFromMps(parser.nextDouble()));
        position.setCourse(parser.nextDouble());

        if (parser.hasNext(3)) {
            position.set(Position.KEY_INPUT, parser.nextHexInt());
            position.set(Position.KEY_EVENT, parser.nextInt());
            position.set(Position.KEY_ODOMETER, parser.nextInt());
        }

        position.set(Position.KEY_BATTERY, parser.nextInt() * 0.001);

        return position;
    }

}
