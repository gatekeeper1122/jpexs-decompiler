/*
 *  Copyright (C) 2010-2013 JPEXS
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.jpexs.decompiler.flash.tags;

import com.jpexs.decompiler.flash.DisassemblyListener;
import com.jpexs.decompiler.flash.SWF;
import com.jpexs.decompiler.flash.SWFInputStream;
import com.jpexs.decompiler.flash.SWFOutputStream;
import com.jpexs.decompiler.flash.action.Action;
import com.jpexs.decompiler.flash.action.ActionListReader;
import com.jpexs.decompiler.flash.helpers.HilightedTextWriter;
import com.jpexs.decompiler.flash.tags.base.ASMSource;
import com.jpexs.decompiler.flash.tags.base.CharacterIdTag;
import com.jpexs.decompiler.graph.ExportMode;
import com.jpexs.helpers.Helper;
import com.jpexs.helpers.ReReadableInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DoInitActionTag extends CharacterIdTag implements ASMSource {

    /**
     * Identifier of Sprite
     */
    public int spriteId = 0;
    /**
     * List of actions to perform
     */
    //public List<Action> actions = new ArrayList<Action>();
    public byte[] actionBytes;
    public static final int ID = 59;

    /**
     * Constructor
     *
     * @param swf
     * @param data Data bytes
     * @param version SWF version
     * @param pos
     * @throws IOException
     */
    public DoInitActionTag(SWF swf, byte[] data, int version, long pos) throws IOException {
        super(swf, ID, "DoInitAction", data, pos);
        SWFInputStream sis = new SWFInputStream(new ByteArrayInputStream(data), version);
        spriteId = sis.readUI16();
        //actions = sis.readActionList();
        actionBytes = sis.readBytes(sis.available());
    }

    /**
     * Gets data bytes
     *
     * @param version SWF version
     * @return Bytes of data
     */
    @Override
    public byte[] getData(int version) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        SWFOutputStream sos = new SWFOutputStream(baos, version);
        try {
            sos.writeUI16(spriteId);
            sos.write(actionBytes);
            //sos.write(Action.actionsToBytes(actions, true, version));
            sos.close();
        } catch (IOException e) {
        }
        return baos.toByteArray();
    }

    /**
     * Whether or not this object contains ASM source
     *
     * @return True when contains
     */
    @Override
    public boolean containsSource() {
        return true;
    }

    /**
     * Converts actions to ASM source
     *
     * @param version SWF version
     * @return ASM source
     */
    @Override
    public HilightedTextWriter getASMSource(int version, ExportMode exportMode, HilightedTextWriter writer, List<Action> actions) {
        if (actions == null) {
            actions = getActions(version);
        }
        return Action.actionsToString(listeners, 0, actions, null, version, exportMode, writer, getPos() + 2, toString()/*FIXME?*/);
    }

    @Override
    public List<Action> getActions(int version) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int prevLength = 0;
            if (previousTag != null) {
                byte[] prevData = previousTag.getData(version);
                baos.write(prevData);
                prevLength = prevData.length;
                baos.write(0);
                baos.write(0);
                prevLength += 2;
                byte[] header = SWFOutputStream.getTagHeader(this, data, version);
                baos.write(header);
                prevLength += header.length;
            }
            baos.write(actionBytes);
            ReReadableInputStream rri = new ReReadableInputStream(new ByteArrayInputStream(baos.toByteArray()));
            rri.setPos(prevLength);
            List<Action> list = ActionListReader.readActionList(listeners, getPos() + 2 - prevLength, rri, version, prevLength, -1, toString()/*FIXME?*/);
            return list;
        } catch (Exception ex) {
            Logger.getLogger(DoActionTag.class.getName()).log(Level.SEVERE, null, ex);
            return new ArrayList<>();
        }
    }

    @Override
    public void setActions(List<Action> actions, int version) {
        actionBytes = Action.actionsToBytes(actions, true, version);
    }

    @Override
    public byte[] getActionBytes() {
        return actionBytes;
    }

    @Override
    public void setActionBytes(byte[] actionBytes) {
        this.actionBytes = actionBytes;
    }

    @Override
    public HilightedTextWriter getActionBytesAsHex(HilightedTextWriter writer) {
        return Helper.byteArrayToHex(writer, actionBytes);
    }

    @Override
    public int getCharacterId() {
        return spriteId;
    }
    List<DisassemblyListener> listeners = new ArrayList<>();

    @Override
    public void addDisassemblyListener(DisassemblyListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeDisassemblyListener(DisassemblyListener listener) {
        listeners.remove(listener);
    }

    @Override
    public String getExportFileName(List<Tag> tags) {
        String expName = getExportName();
        if ((expName == null) || expName.equals("")) {
            return super.toString();
        }
        String[] pathParts;
        if (expName.contains(".")) {
            pathParts = expName.split("\\.");
        } else {
            pathParts = new String[]{expName};
        }
        return Helper.makeFileName(pathParts[pathParts.length - 1]);
    }

    @Override
    public String toString() {
        String expName = getExportName();
        if ((expName == null) || expName.equals("")) {
            return super.toString();
        }
        String[] pathParts;
        if (expName.contains(".")) {
            pathParts = expName.split("\\.");
        } else {
            pathParts = new String[]{expName};
        }
        return pathParts[pathParts.length - 1];
    }

    @Override
    public String getActionSourcePrefix() {
        return "";
    }

    @Override
    public String getActionSourceSuffix() {
        return "";
    }

    @Override
    public int getActionSourceIndent() {
        return 0;
    }
}
