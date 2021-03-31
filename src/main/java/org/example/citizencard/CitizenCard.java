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

import org.example.citizencard.entities.CitizenCardData;
import org.example.citizencard.entities.ReadingStatus;
import pteidlib.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * The Class CitizenCard.
 *
 * @author Alexandre
 */
public class CitizenCard {
    static {
        try {
            System.loadLibrary("pteidlibj");
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Native code library failed to load. \n" + e);
        }
    }

    private static final String PIC_PATH = "pictures";
    private static CitizenCard instance;
    private final Timer cardCheckTimer = new Timer(true);
    private final List<ICitizenCardEventListener> listeners = new ArrayList<ICitizenCardEventListener>();
    private static CitizenCardData ccData;
    private static ReadingStatus.Status ccStatus;

    /**
     * Instantiates a new citizen card.
     */
    private CitizenCard() {
        ccData = null;
        ccStatus = null;
    }

    /**
     * Inits the.
     */
    public static void init() {
        instance = new CitizenCard();
        instance.cardCheckTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    instance.checkCard();
                } catch(Throwable t) {
                    //System.out.println(t.getMessage());
                }
            }
        }, 0, 1000);
    }

    /**
     * Gets the single instance of CitizenCard.
     *
     * @return single instance of CitizenCard
     */
    public static CitizenCard getInstance() {
        return instance;
    }

    /**
     * Adds the listener.
     *
     * @param toAdd
     *            the to add
     */
    public void addListener(ICitizenCardEventListener toAdd) {
        listeners.add(toAdd);
    }
    
    /**
     * Check card.
     *
     * @return true, if successful
     */
    private void checkCard() {
        try {
            pteid.Init("");
            pteid.SetSODChecking(false);
            
            PTEID_TokenInfo tokenInfo = pteid.GetTokenInfo();
            final String token = tokenInfo.serial;
            if (ccData != null && ccData.getToken().equals(token)) {
                return;
            }
            if (token != null) {
                ccStatus = ReadingStatus.Status.READING;
                sendNotification(null, ccStatus);
            }            
            ccData = new CitizenCardData();
            ccData.setToken(token);
            while (ccData.getFirstName() == null) {
                PTEID_ID idData = pteid.GetID();
                System.out.println(idData.firstname);
                ccData.setFirstName(idData.firstname);
                ccData.setSurname(idData.name);
                ccData.setNif(idData.numNIF);
                ccStatus = ReadingStatus.Status.READ;
            }
            ccStatus = ReadingStatus.Status.READ;
            sendNotification(ccData, ccStatus);
            
            PTEID_PIC picData = pteid.GetPic();
            if (null != picData) {
                try {
                    savePhoto(picData.picture, ccData.getNif());
                } catch (FileNotFoundException excep) {
                    System.out.println(excep.getMessage());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            
            System.out.println(ccData);
        } catch (PteidException ex) {
            ccData = null; 
            System.out.println("Card not present. Error: " + ex.getStatus());
            ccStatus = ReadingStatus.Status.ERROR;
            int errorNumber = Integer.parseInt(ex.getMessage().split("Error code : -")[1]);
            for (ReadingStatus.Status status : ReadingStatus.Status.values()) {
                if (status.getErrorCode() == errorNumber) {
                    ccStatus = status;
                }
            }
            sendNotification(null, ccStatus);
        } finally {
            try {
                pteid.Exit(pteid.PTEID_EXIT_LEAVE_CARD);
            } catch (PteidException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Gets the data.
     *
     * @return the data
     */
    public CitizenCardData getData() {
        return ccData;
    }
    
    /**
     * Send notification.
     *
     * @param data
     *            the data
     */
    private void sendNotification(final CitizenCardData data, final  ReadingStatus.Status status) {
        if (listeners != null) {
            for (ICitizenCardEventListener listener : listeners) {
                listener.cardChangedEvent(data, status);
            }
        }
    }

    /**
     * Save photo.
     *
     * @param picture
     *            the picture
     * @param photoName
     *            the photo name
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    private void savePhoto(final byte[] picture, final String photoName) throws IOException {

        InputStream in = new ByteArrayInputStream(picture);       
        BufferedImage img = ImageIO.read(in);
        
        File f = new File(PIC_PATH, photoName + "." + "bmp");
        if (!f.exists()) {
            f.mkdirs();
            f.createNewFile();
        }
        ImageIO.write(img, "bmp", f);
        System.out.println(f);
        ImageIO.read(f);
    }
}

