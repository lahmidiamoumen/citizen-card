/*
 * Copyright (c) 2015 Alexandre Almeida.
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
package org.example.citizencard;


import org.codehaus.jackson.map.ObjectMapper;
import org.example.citizencard.entities.CitizenCardData;
import org.example.citizencard.entities.ReadingStatus;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.*;

/**
 * The Class CitizenCardWebSocket.
 *
 * @author Alexandre
 */
@ServerEndpoint("/citizensocket")
public class CitizenCardWebSocket {

    private static final Set<Session> SESSIONS = Collections.synchronizedSet(new HashSet<Session>());

    /**
     * Instantiates a new citizen card web socket.
     */
    public CitizenCardWebSocket() {
        CitizenCard.getInstance().addListener(new CitizenCardEvent());
    }

    /**
     * On open.
     *
     * @param session
     *            the session
     * @throws EncodeException
     * @throws IOException
     */
    @OnOpen
    public void onOpen(final Session session) throws IOException, EncodeException {
        System.out.println(session.getId() + " has opened a connection");
        SESSIONS.add(session);
        session.getBasicRemote().sendObject("{\"data\":{\"cardInserted\":false}}");
    }

    /**
     * On message.
     *
     * @param message
     *            the message
     * @param session
     *            the session
     */
    @OnMessage
    public void onMessage(String message, Session session) {
    }

    /**
     * On close.
     *
     * @param session
     *            the session
     */
    @OnClose
    public void onClose(Session session) {
        System.out.println("Session " + session.getId() + " has ended");
    }

    /**
     * Send message to all.
     *
     * @param message
     *            the message
     */
    private void sendMessageToAll(final String message) {
        if (SESSIONS.isEmpty()) {
            return;
        }
        for (Session s : SESSIONS) {
            try {
                s.getBasicRemote().sendObject(message);
            } catch (IOException | EncodeException ex) {
                ex.printStackTrace();
            }
        }
    }

    public class CitizenCardEvent implements ICitizenCardEventListener {

        /*
         * (non-Javadoc)
         * 
         * @see
         * com.aalmeida.citizencard.ICitizenCardEventListener#cardChangedEvent
         * (com.aalmeida.citizencard.entities.CitizenCardData, boolean)
         */
        @Override
        public void cardChangedEvent(CitizenCardData ccData, ReadingStatus.Status status) {
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("status", status);
            if (status == ReadingStatus.Status.READ) {
                map.put("data", CitizenCard.getInstance().getData());
            }
            try {
                sendMessageToAll(objectMapper.writeValueAsString(map));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}
