/*
 * *** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is part of dcm4che, an implementation of DICOM(TM) in
 * Java(TM), hosted at https://github.com/gunterze/dcm4che.
 *
 * The Initial Developer of the Original Code is
 * J4Care.
 * Portions created by the Initial Developer are Copyright (C) 2013
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * See @authors listed below
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * *** END LICENSE BLOCK *****
 */

package org.dcm4chee.arc.conf;

import org.dcm4che3.util.StringUtils;

import java.util.Calendar;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @since Jan 2016
 */
public class DeleterThreshold implements Comparable<DeleterThreshold> {
    private final String value;
    private final ScheduleExpression schedule;
    private final long minUsableSpace;

    public DeleterThreshold(String s) {
        this.value = s;
        String[] split1 = StringUtils.split(value, ']');
        switch (split1.length) {
            case 1:
                this.schedule = null;
                break;
            case 2:
                String[] split2 = StringUtils.split(split1[0], '[');
                if (split2.length == 2) {
                    this.schedule = ScheduleExpression.valueOf(split2[1]);
                    break;
                }
            default:
                throw new IllegalArgumentException(s);
        }
        this.minUsableSpace = BinaryPrefix.parse(split1[split1.length-1]);
    }

    public boolean match(Calendar cal) {
        return schedule == null || schedule.contains(cal);
    }

    public long getMinUsableDiskSpace() {
        return minUsableSpace;
    }

    public String toString() {
        return value;
    }

    @Override
    public int compareTo(DeleterThreshold o) {
        return schedule != null ? o.schedule != null ? value.compareTo(o.value) : -1 : o.schedule != null ? 1 : 0;
    }
}
