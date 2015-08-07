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
 * Portions created by the Initial Developer are Copyright (C) 2015
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

package org.dcm4chee.archive.patient.impl;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.IDWithIssuer;
import org.dcm4che3.data.Issuer;
import org.dcm4che3.net.Device;
import org.dcm4che3.soundex.FuzzyStr;
import org.dcm4chee.archive.conf.ArchiveDeviceExtension;
import org.dcm4chee.archive.conf.AttributeFilter;
import org.dcm4chee.archive.conf.Entity;
import org.dcm4chee.archive.entity.IssuerEntity;
import org.dcm4chee.archive.entity.Patient;
import org.dcm4chee.archive.entity.PatientID;
import org.dcm4chee.archive.issuer.IssuerService;
import org.dcm4chee.archive.patient.NonUniquePatientException;
import org.dcm4chee.archive.patient.PatientService;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.Iterator;
import java.util.List;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @since Jul 2015
 */
@Stateless
public class PatientServiceEJB implements PatientService {

    @PersistenceContext(unitName="dcm4chee-arc")
    private EntityManager em;

    @Inject
    private IssuerService issuerService;

    @Inject
    private Device device;

    @Override
    public Patient findPatient(Attributes attrs) throws NonUniquePatientException {
        IDWithIssuer idWithIssuer = IDWithIssuer.pidOf(attrs);
        if (idWithIssuer == null)
            throw new NonUniquePatientException("No Patient ID in received object");

        List<Patient> list = em.createNamedQuery(Patient.FIND_BY_PATIENT_ID_EAGER, Patient.class)
                .setParameter(1, idWithIssuer.getID())
                .getResultList();
        Issuer issuer = idWithIssuer.getIssuer();
        removeNonMatchingIssuer(list, issuer);
        if (list.isEmpty())
            return null;

        if (list.size() > 1) {
            if (issuer != null) {
                removeWithoutIssuer(list);
                if (list.size() == 1)
                    return list.get(0);
            }
            throw new NonUniquePatientException("Multiple Patients with ID " + idWithIssuer);
        }
        return list.get(0);
    }

    private void removeWithoutIssuer(List<Patient> list) {
        for (Iterator<Patient> it = list.iterator(); it.hasNext();) {
            IssuerEntity ie = it.next().getPatientID().getIssuer();
            if (ie == null)
                it.remove();
        }
    }

    private void removeNonMatchingIssuer(List<Patient> list, Issuer issuer) {
        if (issuer != null) {
            for (Iterator<Patient> it = list.iterator(); it.hasNext();) {
                IssuerEntity ie = it.next().getPatientID().getIssuer();
                if (ie != null && ie.getIssuer().matches(issuer))
                    it.remove();
            }
        }
    }

    @Override
    public Patient createPatient(Attributes attrs) {
        Patient patient = new Patient();
        patient.setAttributes(attrs, getAttributeFilter(), getFuzzyStr());
        patient.setPatientID(createPatientID(IDWithIssuer.pidOf(attrs)));
        em.persist(patient);
        return patient;
    }

    private PatientID createPatientID(IDWithIssuer idWithIssuer) {
        if (idWithIssuer == null)
            return null;

        PatientID patientID = new PatientID();
        patientID.setID(idWithIssuer.getID());
        if (idWithIssuer.getIssuer() != null)
            patientID.setIssuer(issuerService.findOrCreate(idWithIssuer.getIssuer()));

        return patientID;
    }

    private ArchiveDeviceExtension getArchiveDeviceExtension() {
        return device.getDeviceExtension(ArchiveDeviceExtension.class);
    }

    public AttributeFilter getAttributeFilter() {
        return getArchiveDeviceExtension().getAttributeFilter(Entity.Patient);
    }

    public FuzzyStr getFuzzyStr() {
        return getArchiveDeviceExtension().getFuzzyStr();
    }
}