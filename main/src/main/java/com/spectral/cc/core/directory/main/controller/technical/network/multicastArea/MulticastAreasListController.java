/**
 * Directory Main bundle
 * Directories MulticastArea RUD Controller
 * Copyright (C) 2013 Mathilde Ffrench
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.spectral.cc.core.directory.main.controller.technical.network.multicastArea;

import com.spectral.cc.core.directory.commons.consumer.JPAProviderConsumer;
import com.spectral.cc.core.directory.main.controller.technical.network.datacenter.DatacentersListController;
import com.spectral.cc.core.directory.main.controller.technical.network.lan.LansListController;
import com.spectral.cc.core.directory.commons.model.technical.network.Datacenter;
import com.spectral.cc.core.directory.commons.model.technical.network.Lan;
import com.spectral.cc.core.directory.commons.model.technical.network.MulticastArea;
import org.primefaces.event.ToggleEvent;
import org.primefaces.model.LazyDataModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.transaction.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@ManagedBean
@SessionScoped
public class MulticastAreasListController implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(MulticastAreasListController.class);

    private HashMap<Long, MulticastArea> rollback = new HashMap<Long, MulticastArea>();

    private LazyDataModel<MulticastArea> lazyModel ;
    private MulticastArea[]              selectedMareaList ;

    private HashMap<Long,String>           addedDC    = new HashMap<Long, String>();
    private HashMap<Long,List<Datacenter>> removedDCs = new HashMap<Long, List<Datacenter>>();

    private HashMap<Long,String>    addedLAN    = new HashMap<Long, String>();
    private HashMap<Long,List<Lan>> removedLANs = new HashMap<Long, List<Lan>>();

    @PostConstruct
    private void init() {
        lazyModel = new MulticastAreaLazyModel().setEntityManager(JPAProviderConsumer.getInstance().getJpaProvider().getSharedEM());
    }

    /*
     * PrimeFaces table tools
     */
    public LazyDataModel<MulticastArea> getLazyModel() {
        return lazyModel;
    }

    public MulticastArea[] getSelectedMareaList() {
        return selectedMareaList;
    }

    public void setSelectedMareaList(MulticastArea[] selectedMareaList) {
        this.selectedMareaList = selectedMareaList;
    }

    /*
     * Multicast Area update tools
     */
    public HashMap<Long, MulticastArea> getRollback() {
        return rollback;
    }

    public void setRollback(HashMap<Long, MulticastArea> rollback) {
        this.rollback = rollback;
    }

    public HashMap<Long, String> getAddedLAN() {
        return addedLAN;
    }

    public void setAddedLAN(HashMap<Long, String> addedLAN) {
        this.addedLAN = addedLAN;
    }

    public void syncAddedLan(MulticastArea marea) throws NotSupportedException, SystemException {
        for (Lan lan: LansListController.getAll()) {
            if (lan.getName().equals(this.addedLAN.get(marea.getId()))) {
                marea.getLans().add(lan);
            }
        }
    }

    public HashMap<Long, List<Lan>> getRemovedLANs() {
        return removedLANs;
    }

    public void setRemovedLANs(HashMap<Long, List<Lan>> removedLANs) {
        this.removedLANs = removedLANs;
    }

    public void syncRemovedLans(MulticastArea marea) throws NotSupportedException, SystemException {
        List<Lan> lans2beRM = this.removedLANs.get(marea.getId());
        for (Lan lan2beRM : lans2beRM) {
            marea.getLans().remove(lan2beRM);
        }
    }

    public HashMap<Long, String> getAddedDC() {
        return addedDC;
    }

    public void setAddedDC(HashMap<Long, String> addedDC) {
        this.addedDC = addedDC;
    }

    public void syncAddedDC(MulticastArea marea) throws NotSupportedException, SystemException {
        for (Datacenter dc: DatacentersListController.getAll()) {
            if (dc.getName().equals(this.addedDC.get(marea.getId()))) {
                marea.getDatacenters().add(dc);
            }
        }
    }

    public HashMap<Long, List<Datacenter>> getRemovedDCs() {
        return removedDCs;
    }

    public void setRemovedDCs(HashMap<Long, List<Datacenter>> removedDCs) {
        this.removedDCs = removedDCs;
    }

    public void syncRemovedDCs(MulticastArea marea) throws NotSupportedException, SystemException {
        List<Datacenter> dcs2beRM = this.removedDCs.get(marea.getId());
        for (Datacenter dc2beRM : dcs2beRM) {
            marea.getDatacenters().remove(dc2beRM);
        }
    }

    public void onRowToggle(ToggleEvent event) throws CloneNotSupportedException {
        log.debug("Row Toogled : {}", new Object[]{event.getVisibility().toString()});
        MulticastArea eventMarea = ((MulticastArea) event.getData());
        if (event.getVisibility().toString().equals("HIDDEN")) {
            log.debug("EDITION MODE CLOSED: remove eventMarea {} clone from rollback map...", eventMarea.getId());
            rollback.remove(eventMarea.getId());
            addedDC.remove(eventMarea.getId());
            removedDCs.remove(eventMarea.getId());
            addedLAN.remove(eventMarea.getId());
            removedLANs.remove(eventMarea.getId());
        } else {
            log.debug("EDITION MODE OPEN: store current eventMarea {} clone into rollback map...", eventMarea.getId());
            rollback.put(eventMarea.getId(), eventMarea.clone());
            addedDC.put(eventMarea.getId(), "");
            removedDCs.put(eventMarea.getId(), new ArrayList<Datacenter>());
            addedLAN.put(eventMarea.getId(), "");
            removedLANs.put(eventMarea.getId(), new ArrayList<Lan>());
        }
    }

    public void update(MulticastArea multicastArea) throws SystemException, NotSupportedException, HeuristicRollbackException, HeuristicMixedException, RollbackException {
        try {
            //JPAProviderConsumer.getInstance().getJpaProvider().getSharedUX().begin();
            //JPAProviderConsumer.getInstance().getJpaProvider().getSharedEM().joinTransaction();
            JPAProviderConsumer.getInstance().getJpaProvider().getSharedEM().getTransaction().begin();
            JPAProviderConsumer.getInstance().getJpaProvider().getSharedEM().merge(multicastArea);
            for (Datacenter dc : multicastArea.getDatacenters()){
                if (!rollback.get(multicastArea.getId()).getDatacenters().contains(dc)) {
                    dc.getMulticastAreas().add(multicastArea);
                    JPAProviderConsumer.getInstance().getJpaProvider().getSharedEM().merge(dc);
                }
            }
            for (Datacenter dc : rollback.get(multicastArea.getId()).getDatacenters()) {
                if (!multicastArea.getDatacenters().contains(dc)) {
                    dc.getMulticastAreas().remove(multicastArea);
                    JPAProviderConsumer.getInstance().getJpaProvider().getSharedEM().merge(dc);
                }
            }
            for (Lan lan : multicastArea.getLans()) {
                if (!rollback.get(multicastArea.getId()).getLans().contains(lan)){
                    MulticastArea previousLanMarea = lan.getMarea();
                    if (previousLanMarea!=null && previousLanMarea.getName()!=multicastArea.getName()) {
                        previousLanMarea.getLans().remove(lan);
                        JPAProviderConsumer.getInstance().getJpaProvider().getSharedEM().merge(previousLanMarea);
                    }
                    lan.setMarea(multicastArea);
                    JPAProviderConsumer.getInstance().getJpaProvider().getSharedEM().merge(lan);
                }
            }
            for (Lan lan : rollback.get(multicastArea.getId()).getLans()) {
                if (!multicastArea.getLans().contains(lan)) {
                    lan.setMarea(null);
                    JPAProviderConsumer.getInstance().getJpaProvider().getSharedEM().merge(lan);
                }
            }
            //JPAProviderConsumer.getInstance().getJpaProvider().getSharedUX().commit();
            JPAProviderConsumer.getInstance().getJpaProvider().getSharedEM().getTransaction().commit();
            rollback.put(multicastArea.getId(), multicastArea);
            FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_INFO,
                                                       "Multicast area updated successfully !",
                                                       "Multicast area name : " + multicastArea.getName());
            FacesContext.getCurrentInstance().addMessage(null, msg);
        } catch (Throwable t) {
            log.debug("Throwable catched !");
            t.printStackTrace();
            FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                                                       "Throwable raised while updeting multicast area " + rollback.get(multicastArea.getId()).getName() + " !",
                                                       "Throwable message : " + t.getMessage());
            FacesContext.getCurrentInstance().addMessage(null, msg);
            if(JPAProviderConsumer.getInstance().getJpaProvider().getSharedEM().getTransaction().isActive())
                JPAProviderConsumer.getInstance().getJpaProvider().getSharedEM().getTransaction().rollback();
            /*
            FacesMessage msg2;
            int txStatus = JPAProviderConsumer.getInstance().getJpaProvider().getSharedUX().getStatus();
            switch(txStatus) {
                case Status.STATUS_NO_TRANSACTION:
                    msg2 = new FacesMessage(FacesMessage.SEVERITY_WARN,
                                                   "Operation canceled !",
                                                   "Operation : multicast area " + rollback.get(multicastArea.getId()).getName() + " update.");
                    break;
                case Status.STATUS_MARKED_ROLLBACK:
                    try {
                        log.debug("Rollback operation !");
                        JPAProviderConsumer.getInstance().getJpaProvider().getSharedUX().rollback();
                        msg2 = new FacesMessage(FacesMessage.SEVERITY_WARN,
                                                                    "Operation rollbacked !",
                                                                    "Operation : multicast area " + rollback.get(multicastArea.getId()) + " update.");

                    } catch (Throwable t2) {
                        t2.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                        msg2 = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                                                                    "Error while rollbacking operation !",
                                                                    "Operation : multicast area " + rollback.get(multicastArea.getId()) + " update.");
                    }
                    break;
                default:
                    msg2 = new FacesMessage(FacesMessage.SEVERITY_WARN,
                                                   "Operation canceled ! ("+txStatus+")",
                                                   "Operation : multicast area " + rollback.get(multicastArea.getId()) + " update.");
                    break;
            }
            FacesContext.getCurrentInstance().addMessage(null, msg2);
            */
        }
    }

    /*
     * Multicast Area delete tool
     */
    public void delete() {
        log.debug("Remove selected Multicast Area !");
        for (MulticastArea marea2BeRemoved: selectedMareaList) {
            try {
                //JPAProviderConsumer.getInstance().getJpaProvider().getSharedUX().begin();
                //JPAProviderConsumer.getInstance().getJpaProvider().getSharedEM().joinTransaction();
                JPAProviderConsumer.getInstance().getJpaProvider().getSharedEM().getTransaction().begin();
                for (Lan lan : marea2BeRemoved.getLans()) {
                    lan.setMarea(null);
                    JPAProviderConsumer.getInstance().getJpaProvider().getSharedEM().merge(lan);
                }
                for (Datacenter dc : marea2BeRemoved.getDatacenters()) {
                    dc.getMulticastAreas().remove(marea2BeRemoved);
                    JPAProviderConsumer.getInstance().getJpaProvider().getSharedEM().merge(dc.getMulticastAreas());
                }
                JPAProviderConsumer.getInstance().getJpaProvider().getSharedEM().remove(marea2BeRemoved);
                //JPAProviderConsumer.getInstance().getJpaProvider().getSharedUX().commit();
                JPAProviderConsumer.getInstance().getJpaProvider().getSharedEM().getTransaction().commit();
                FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_INFO,
                                                           "Multicast area deleted successfully !",
                                                           "Multicast area name : " + marea2BeRemoved.getName());
                FacesContext.getCurrentInstance().addMessage(null, msg);
            } catch (Throwable t) {
                log.debug("Throwable catched !");
                t.printStackTrace();
                FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                                                           "Throwable raised while creating multicast area " + marea2BeRemoved.getName() + " !",
                                                           "Throwable message : " + t.getMessage());
                FacesContext.getCurrentInstance().addMessage(null, msg);
                if (JPAProviderConsumer.getInstance().getJpaProvider().getSharedEM().getTransaction().isActive())
                    JPAProviderConsumer.getInstance().getJpaProvider().getSharedEM().getTransaction().rollback();
/*
                try {
                    FacesMessage msg2;
                    int txStatus = JPAProviderConsumer.getInstance().getJpaProvider().getSharedUX().getStatus();
                    switch(txStatus) {
                        case Status.STATUS_NO_TRANSACTION:
                            msg2 = new FacesMessage(FacesMessage.SEVERITY_WARN,
                                                           "Operation canceled !",
                                                           "Operation : multicast area " + marea2BeRemoved.getName() + " deletion.");
                            break;
                        case Status.STATUS_MARKED_ROLLBACK:
                            try {
                                log.debug("Rollback operation !");
                                JPAProviderConsumer.getInstance().getJpaProvider().getSharedUX().rollback();
                                msg2 = new FacesMessage(FacesMessage.SEVERITY_WARN,
                                                               "Operation rollbacked !",
                                                               "Operation : multicast area " + marea2BeRemoved.getName() + " deletion.");
                                FacesContext.getCurrentInstance().addMessage(null, msg2);
                            } catch (SystemException e) {
                                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                                msg2 = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                                                               "Error while rollbacking operation !",
                                                               "Operation : multicast area " + marea2BeRemoved.getName() + " deletion.");
                                FacesContext.getCurrentInstance().addMessage(null, msg2);
                            }
                            break;
                        default:
                            msg2 = new FacesMessage(FacesMessage.SEVERITY_WARN,
                                                           "Operation canceled ! ("+txStatus+")",
                                                           "Operation : multicast area " + marea2BeRemoved.getName() + " deletion.");
                            break;
                    }
                    FacesContext.getCurrentInstance().addMessage(null, msg2);
                } catch (SystemException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
*/
            }
        }
        selectedMareaList=null;
    }

    /*
     * Multicast Area join tool
     */
    public static List<MulticastArea> getAll() throws SystemException, NotSupportedException {
        CriteriaBuilder builder = JPAProviderConsumer.getInstance().getJpaProvider().getSharedEM().getCriteriaBuilder();
        CriteriaQuery<MulticastArea> criteria = builder.createQuery(MulticastArea.class);
        Root<MulticastArea> root = criteria.from(MulticastArea.class);
        criteria.select(root).orderBy(builder.asc(root.get("name")));
        return JPAProviderConsumer.getInstance().getJpaProvider().getSharedEM().createQuery(criteria).getResultList();
    }

    public static List<MulticastArea> getAllForSelector() throws SystemException, NotSupportedException {
        CriteriaBuilder builder  = JPAProviderConsumer.getInstance().getJpaProvider().getSharedEM().getCriteriaBuilder();
        CriteriaQuery<MulticastArea> criteria = builder.createQuery(MulticastArea.class);
        Root<MulticastArea> root = criteria.from(MulticastArea.class);
        criteria.select(root).orderBy(builder.asc(root.get("name")));

        List<MulticastArea> list =  JPAProviderConsumer.getInstance().getJpaProvider().getSharedEM().createQuery(criteria).getResultList();
        list.add(0, new MulticastArea().setNameR("No multicast area"));
        list.add(0, new MulticastArea().setNameR("Select multicast area for this lan"));
        return list;
    }

    public static List<MulticastArea> getAllForInplace() throws SystemException, NotSupportedException {
        CriteriaBuilder builder  = JPAProviderConsumer.getInstance().getJpaProvider().getSharedEM().getCriteriaBuilder();
        CriteriaQuery<MulticastArea> criteria = builder.createQuery(MulticastArea.class);
        Root<MulticastArea> root = criteria.from(MulticastArea.class);
        criteria.select(root).orderBy(builder.asc(root.get("name")));

        List<MulticastArea> list =  JPAProviderConsumer.getInstance().getJpaProvider().getSharedEM().createQuery(criteria).getResultList();
        list.add(0, new MulticastArea().setNameR("No multicast area"));
        return list;
    }
}