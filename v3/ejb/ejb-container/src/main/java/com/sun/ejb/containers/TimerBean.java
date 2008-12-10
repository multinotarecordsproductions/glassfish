/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
 * 
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License. You can obtain
 * a copy of the License at https://glassfish.dev.java.net/public/CDDL+GPL.html
 * or glassfish/bootstrap/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 * 
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.  If applicable, add the following below the License
 * Header, with the fields enclosed by brackets [] replaced by your own
 * identifying information: "Portions Copyrighted [year]
 * [name of copyright owner]"
 * 
 * Contributor(s):
 * 
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package com.sun.ejb.containers;

import java.io.Serializable;
import java.io.IOException;

import java.util.Date;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;

import javax.ejb.EJBContext;
import javax.ejb.EntityContext;
import javax.ejb.CreateException;
import javax.ejb.FinderException;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.TimerConfig;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.PersistenceContext;

import java.util.logging.Logger;
import java.util.logging.Level;

import com.sun.logging.LogDomains;

import com.sun.ejb.containers.EjbContainerUtilImpl;
import com.sun.enterprise.deployment.EjbDescriptor;

import javax.naming.InitialContext;
import javax.sql.DataSource;
import java.sql.Connection;

/**
 * TimerBean is a facade for the persistent state of an EJB Timer.
 * It is part of the EJB container and is implemented using 
 * Java Persistence API.  The standard JPA behavior is useful in 
 * implementing the transactional properties of EJB timers.
 * When an EJB timer is created by an application, it is not
 * eligible for expiration until the transaction commits.
 * Likewise, if a timer is cancelled and the transaction rolls
 * back, the timer must be reactivated.
 * To accomplish this, TimerBean registers callbacks with the
 * transaction manager and interacts with the EJBTimerService
 * accordingly.  
 *
 * @author Kenneth Saks
 * @author Marina Vatkina
 */
@Stateless
public class TimerBean implements TimerLocal {

    private static final Logger logger = LogDomains.getLogger(TimerBean.class, LogDomains.EJB_LOGGER);

    private EJBContextImpl context_;

    @PersistenceContext(unitName="__EJB__Timer__App")
    private EntityManager em;

    // Find Timer by Id
    public TimerState findTimer(TimerPrimaryKey timerId) {
        return em.find(TimerState.class, timerId);
    }

    //
    // Query methods for timer ids
    //
    
    public Set findTimerIdsByContainer(long containerId) {
        Query q = em.createNamedQuery("findTimerIdsByContainer");
        q.setParameter(1, containerId);
        return toPKeys(q.getResultList());
    }

    public Set findTimerIdsByContainerAndState
        (long containerId, int state)  {
        Query q = em.createNamedQuery("findTimerIdsByContainerAndState");
        q.setParameter(1, containerId);
        q.setParameter(2, state);
        return toPKeys(q.getResultList());
    }

    public Set findTimerIdsByContainerAndOwner
        (long containerId, String ownerId) {
        Query q = em.createNamedQuery("findTimerIdsByContainerAndOwner");
        q.setParameter(1, containerId);
        q.setParameter(2, ownerId);
        return toPKeys(q.getResultList());
    }

    public Set findTimerIdsByContainerAndOwnerAndState
        (long containerId, String ownerId, int state)  {
        Query q = em.createNamedQuery("findTimerIdsByContainerAndOwnerAndState");
        q.setParameter(1, containerId);
        q.setParameter(2, ownerId);
        q.setParameter(3, state);
        return toPKeys(q.getResultList());
    }

    public Set findTimerIdsByOwner(String ownerId)  {
        Query q = em.createNamedQuery("findTimerIdsByOwner");
        q.setParameter(1, ownerId);
        return toPKeys(q.getResultList());
    }

    public Set findTimerIdsByOwnerAndState
        (String ownerId, int state)  {
        Query q = em.createNamedQuery("findTimerIdsByOwnerAndState");
        q.setParameter(1, ownerId);
        q.setParameter(2, state);
        return toPKeys(q.getResultList());
    }
   
    //
    // Query methods for timer beans
    // XXX These methods return Sets XXX
    //

    public Set findTimersByContainer(long containerId) {
        Query q = em.createNamedQuery("findTimersByContainer");
        q.setParameter(1, containerId);
        return new HashSet(q.getResultList());
    }

    public Set findTimersByContainerAndState
        (long containerId, int state) {
        Query q = em.createNamedQuery("findTimersByContainerAndState");
        q.setParameter(1, containerId);
        q.setParameter(2, state);
        return new HashSet(q.getResultList());
    }

    public Set findTimersByContainerAndOwner
        (long containerId, String ownerId) {
        Query q = em.createNamedQuery("findTimersByContainerAndOwner");
        q.setParameter(1, containerId);
        q.setParameter(2, ownerId);
        return new HashSet(q.getResultList());
    }

    public Set findTimersByContainerAndOwnerAndState
        (long containerId, String ownerId, int state) {
        Query q = em.createNamedQuery("findTimersByContainerAndOwnerAndState");
        q.setParameter(1, containerId);
        q.setParameter(2, ownerId);
        q.setParameter(3, state);
        return new HashSet(q.getResultList());
    }

    private Set findTimersByOwner(String ownerId)  {
        Query q = em.createNamedQuery("findTimersByOwner");
        q.setParameter(1, ownerId);
        return new HashSet(q.getResultList());
    }

    public Set findTimersByOwnerAndState
        (String ownerId, int state) {
        Query q = em.createNamedQuery("findTimersByOwnerAndState");
        q.setParameter(1, ownerId);
        q.setParameter(2, state);
        return new HashSet(q.getResultList());
    }


    //
    // Query methods for timer counts
    //
    
    public int countTimersByContainer(long containerId) {
        Query q = em.createNamedQuery("countTimersByContainer");
        q.setParameter(1, containerId);
        return ((Number)q.getSingleResult()).intValue();
    }

    public int countTimersByContainerAndState
        (long containerId, int state) {
        Query q = em.createNamedQuery("countTimersByContainerAndState");
        q.setParameter(1, containerId);
        q.setParameter(2, state);
        return ((Number)q.getSingleResult()).intValue();
    }

    public int countTimersByContainerAndOwner
        (long containerId, String ownerId) {
        Query q = em.createNamedQuery("countTimersByContainerAndOwner");
        q.setParameter(1, containerId);
        q.setParameter(2, ownerId);
        return ((Number)q.getSingleResult()).intValue();
    }

    public int countTimersByContainerAndOwnerAndState
        (long containerId, String ownerId, int state) {
        Query q = em.createNamedQuery("countTimersByContainerAndOwnerAndState");
        q.setParameter(1, containerId);
        q.setParameter(2, ownerId);
        q.setParameter(3, state);
        return ((Number)q.getSingleResult()).intValue();
    }

    public int countTimersByOwner(String ownerId)  {
        Query q = em.createNamedQuery("countTimersByOwner");
        q.setParameter(1, ownerId);
        return ((Number)q.getSingleResult()).intValue();
    }

    public int countTimersByOwnerAndState
        (String ownerId, int state) {
        Query q = em.createNamedQuery("countTimersByOwnerAndState");
        q.setParameter(1, ownerId);
        q.setParameter(2, state);
        return ((Number)q.getSingleResult()).intValue();
    }

    //
    // These data members contain derived state for 
    // some immutable fields.
    //

    // deserialized state from blob
    private boolean blobLoaded_;
    private Object timedObjectPrimaryKey_;
    private transient Serializable info_;

    public TimerState createTimer
        (String timerId, long containerId, String ownerId,
         Object timedObjectPrimaryKey, 
         Date initialExpiration, long intervalDuration, 
         TimerSchedule schedule, TimerConfig timerConfig)
        throws CreateException {

        TimerState timer = null;
        try {
            timer = new TimerState (timerId, containerId, ownerId,
                    timedObjectPrimaryKey, initialExpiration, 
                    intervalDuration, schedule, timerConfig.getInfo());
        } catch(IOException ioe) {
            CreateException ce = new CreateException();
            ce.initCause(ioe);
            throw ce;
        }

        if( logger.isLoggable(Level.FINE) ) {
            logger.log(Level.FINE, "TimerBean.createTimer() ::timerId=" +
                       timer.getTimerId() + " ::containerId=" + timer.getContainerId() + 
                       " ::timedObjectPK=" + timedObjectPrimaryKey +
                       " ::info=" + timerConfig.getInfo() +
                       " ::schedule=" + timer.getSchedule() +
                       " ::persistent=" + timerConfig.isPersistent() +
                       " ::initialExpiration=" + initialExpiration +
                       " ::intervalDuration=" + intervalDuration +
                       " :::state=" + timer.stateToString() + 
                       " :::creationTime="  + timer.getCreationTime() +
                       " :::ownerId=" + timer.getOwnerId()); 
        }

        //
        // Only proceed with transactional semantics if this timer
        // is owned by the current server instance.  NOTE that this
        // will *ALWAYS* be the case for timers created from EJB
        // applications via the javax.ejb.EJBTimerService.create methods.  
        //
        // For testing purposes, ejbCreate takes an ownerId parameter, 
        // which allows us to easily simulate other server instances 
        // by creating timers for them.  In those cases, we don't need
        // the timer transaction semantics and ejbTimeout logic.  Simulating
        // the creation of timers for the same application and different
        // server instances from a script is difficult since the
        // containerId is not generated until after deployment.  
        //
        try {
            getEJBTimerService().addTimerSynchronization(context_, 
                    timerId, initialExpiration, containerId, ownerId);
        } catch(Exception e) {
            CreateException ce = new CreateException();
            ce.initCause(e);
            throw ce;
        }

        em.persist(timer);
        return timer;
    }

    private String getOwnerIdOfThisServer() {
        return getEJBTimerService().getOwnerIdOfThisServer();                
    }

    private static EJBTimerService getEJBTimerService() {
        return EjbContainerUtilImpl.getInstance().getEJBTimerService();
    }

    public void setSessionContext(SessionContext context) {
        context_ = (EJBContextImpl) context;
    }
    
    public void remove(TimerPrimaryKey timerId) {
        TimerState timer = em.find(TimerState.class, timerId);
        if (timer != null) {
            em.remove(timer);
        }
    }
    
    public void remove(Set<TimerPrimaryKey> timerIds) {
        for(TimerPrimaryKey timerId: timerIds) {
            try {
                remove(timerId);
            } catch(Exception e) {
                logger.log(Level.FINE, "Cannot remove timer " + timerId +
                               " for unknown container ", e);
            }
        }
    }
    
    public void cancel(TimerPrimaryKey timerId) 
            throws FinderException, Exception {

        TimerState timer = em.find(TimerState.class, timerId);
        // If timer is null need to throw a FinderException so 
        // that the caller can handle it.
        if( timer == null) {
            throw new FinderException("timer " + timerId + " does not exist");
        }

        // First set the timer to the cancelled state.  This step is
        // performed whether or not the current server instance owns
        // the timer.

        if( timer.getState() == TimerState.CANCELLED ) {
            // already cancelled or removed
            return;
        }

        timer.setState(TimerState.CANCELLED);

        getEJBTimerService().cancelTimerSynchronization(context_, timerId, 
                timer.getContainerId(), timer.getOwnerId());

        // XXX ???? WHY WAS IT: NOTE that it's the caller's responsibility to call remove().
        em.remove(timer);

        return;
    }

    public void cancelTimers(Collection<TimerState> timers) {
        for(TimerState timer : timers) {
            try {
                em.remove(timer);
            } catch(Exception e) {
                logger.log(Level.WARNING, "ejb.cancel_entity_timer",
                           new Object[] { timer.getTimerId() });
                logger.log(Level.WARNING, "", e);
            }
        }
    }

    private Set toPKeys(Collection ids) {
        Set pkeys = new HashSet();
        for(Iterator iter = ids.iterator(); iter.hasNext();) {
            pkeys.add(new TimerPrimaryKey((String) iter.next()));
        }
        return pkeys;
    }

    //
    // Other query methods for timer ids
    //

    public Set findActiveTimerIdsByContainer(long containerId) {
        return findTimerIdsByContainerAndState(containerId, 
                               TimerState.ACTIVE);
    }

    public Set findCancelledTimerIdsByContainer(long containerId) {
        return findTimerIdsByContainerAndState(containerId, 
                                TimerState.CANCELLED);
    }

    public Set findTimerIdsOwnedByThisServerByContainer
        (long containerId) {
        return findTimerIdsByContainerAndOwner
                         (containerId, getOwnerIdOfThisServer());
    }

    public Set findActiveTimerIdsOwnedByThisServerByContainer
        (long containerId) {
        return findTimerIdsByContainerAndOwnerAndState
                       (containerId, getOwnerIdOfThisServer(), 
                               TimerState.ACTIVE);
    }

    public Set findCancelledTimerIdsOwnedByThisServerByContainer
        (long containerId) {
        return findTimerIdsByContainerAndOwnerAndState
                       (containerId, getOwnerIdOfThisServer(), 
                                TimerState.CANCELLED);
    }


    public Set findTimerIdsOwnedByThisServer() {
        return findTimerIdsByOwner(getOwnerIdOfThisServer());
    }
   
    public Set findActiveTimerIdsOwnedByThisServer() {
        return findTimerIdsByOwnerAndState
                       (getOwnerIdOfThisServer(), 
                               TimerState.ACTIVE);
    }

    public Set findCancelledTimerIdsOwnedByThisServer() {
        return findTimerIdsByOwnerAndState
                       (getOwnerIdOfThisServer(), 
                                TimerState.CANCELLED);
    }

    public Set findTimerIdsOwnedBy(String ownerId) {
        return findTimerIdsByOwner(ownerId);
    }

    public Set findActiveTimerIdsOwnedBy(String ownerId) {
        return findTimerIdsByOwnerAndState(ownerId, 
                               TimerState.ACTIVE);
    }

    public Set findCancelledTimerIdsOwnedBy(String ownerId) {
        return findTimerIdsByOwnerAndState(ownerId, 
                                TimerState.CANCELLED);
    }

    //
    // Helper query methods for timer beans
    //

    public Set findActiveTimersByContainer(long containerId) {
        return findTimersByContainerAndState(containerId, 
                               TimerState.ACTIVE);
    }

    public Set findCancelledTimersByContainer(long containerId) {
        return findTimersByContainerAndState
                       (containerId, TimerState.CANCELLED);
    }

    public Set findTimersOwnedByThisServerByContainer
        (long containerId) {
        return findTimersByContainerAndOwner
                         (containerId, getOwnerIdOfThisServer());
    }

    public Set findActiveTimersOwnedByThisServerByContainer
        (long containerId) {
        return findTimersByContainerAndOwnerAndState
                       (containerId, getOwnerIdOfThisServer(), 
                                TimerState.ACTIVE);
    }

    public Set findCancelledTimersOwnedByThisServerByContainer
        (long containerId) {
        return findTimersByContainerAndOwnerAndState
                       (containerId, getOwnerIdOfThisServer(), 
                                TimerState.CANCELLED);
    }


    public Set findTimersOwnedByThisServer() {
        return findTimersByOwner(getOwnerIdOfThisServer());
    }
   
    public Set findActiveTimersOwnedByThisServer() {
        return findTimersByOwnerAndState
                       (getOwnerIdOfThisServer(), 
                                TimerState.ACTIVE);
    }

    public Set findCancelledTimersOwnedByThisServer() {
        return findTimersByOwnerAndState
                       (getOwnerIdOfThisServer(), 
                                TimerState.CANCELLED);
    }

    public Set findTimersOwnedBy(String ownerId) {
        return findTimersByOwner(ownerId);
    }

    public Set findActiveTimersOwnedBy(String ownerId) {
        return findTimersByOwnerAndState(ownerId, 
                                TimerState.ACTIVE);
    }

    public Set findCancelledTimersOwnedBy(String ownerId) {
        return findTimersByOwnerAndState(ownerId, 
                                TimerState.CANCELLED);
    }   


    //
    // Helper query methods for timer counts
    //

    public int countActiveTimersByContainer(long containerId) {
        return countTimersByContainerAndState(containerId, 
                                TimerState.ACTIVE);
    }

    public int countCancelledTimersByContainer(long containerId) {
        return countTimersByContainerAndState(containerId, 
                                TimerState.CANCELLED);
    }

    public int countTimersOwnedByThisServerByContainer
        (long containerId) {
        return countTimersByContainerAndOwner
                         (containerId, getOwnerIdOfThisServer());
    }

    public int countActiveTimersOwnedByThisServerByContainer
        (long containerId) {
        return countTimersByContainerAndOwnerAndState
                       (containerId, getOwnerIdOfThisServer(), 
                                TimerState.ACTIVE);
    }

    public int countCancelledTimersOwnedByThisServerByContainer
        (long containerId) {
        return countTimersByContainerAndOwnerAndState
                       (containerId, getOwnerIdOfThisServer(), 
                                TimerState.CANCELLED);
    }


    public int countTimersOwnedByThisServer() {
        return countTimersByOwner(getOwnerIdOfThisServer());
    }

    public String[] countTimersOwnedByServerIds(String[] serverIds) {
        String[] totalTimers = new String[ serverIds.length ];
        int i = 0;
        for (String serverId : serverIds) {
            totalTimers[i] = Integer.toString(
                    countTimersOwnedBy(serverId));
            i++;
        }

        return totalTimers;
    }
   
    public int countActiveTimersOwnedByThisServer() {
        return countTimersByOwnerAndState
                       (getOwnerIdOfThisServer(), 
                                TimerState.ACTIVE);
    }

    public int countCancelledTimersOwnedByThisServer() {
        return countTimersByOwnerAndState
                       (getOwnerIdOfThisServer(), 
                                TimerState.CANCELLED);
    }

    public int countTimersOwnedBy(String ownerId) {
        return countTimersByOwner(ownerId);
    }

    public int countActiveTimersOwnedBy(String ownerId) {
        return countTimersByOwnerAndState(ownerId, 
                                TimerState.ACTIVE);
    }

    public int countCancelledTimersOwnedBy(String ownerId) {
        return countTimersByOwnerAndState(ownerId, 
                                TimerState.CANCELLED);
    }   

    public boolean checkStatus(String resourceJndiName,
                                      boolean checkDatabase) {

        boolean success = false;

        Connection connection = null;

        try {

            InitialContext ic = new InitialContext();
            
            DataSource dataSource = (DataSource) ic.lookup(resourceJndiName);

            if( checkDatabase ) {
                connection = dataSource.getConnection();
                
                connection.close();
                
                connection = null;
                
                // Now try to a query that will access the timer table itself.
                // Use a query that won't return a lot of data(even if the
                // table is large) to reduce the overhead of this check.
                countTimersByContainer(0);
            }

            success = true;           
                        
        } catch(Exception e) {

            logger.log(Level.WARNING, "ejb.timer_service_init_error", 
                       "");
            // Log exception itself at FINE level.  The most likely cause
            // is a connection error when the database is not started.  This
            // is already logged twice by the jdbc layer.
            logger.log(Level.FINE, "ejb.timer_service_init_error", e);

        } finally {
            if( connection != null ) {
                try {
                    connection.close();
                } catch(Exception e) {
                    logger.log(Level.FINE, "timer connection close exception",
                               e);
                }
            }
        }

        return success;
    }

    public int migrateTimers(String fromOwnerId, String toOwnerId) {
        Query q = em.createNamedQuery("updateTimersFromOwnerToNewOwner");
        q.setParameter("fromOwner", fromOwnerId);
        q.setParameter("toOwner", toOwnerId);
        return q.executeUpdate();
    }

    public static void testCreate(String timerId, EJBContext context,
                                   String ownerId,
                                  Date initialExpiration, 
                                   long intervalDuration, 
                                   Serializable info) throws CreateException {
        
        TimerLocal timerLocal = getEJBTimerService().getTimerLocal();

        EjbDescriptor ejbDesc = ((EJBContextImpl) context).getContainer().getEjbDescriptor();
        long containerId = ejbDesc.getUniqueId();

        Object timedObjectPrimaryKey = (context instanceof EntityContext) ?
                ((EntityContext)context).getPrimaryKey() : null;

        TimerConfig timerConfig = new TimerConfig();
        timerConfig.setInfo(info);
        timerLocal.createTimer(timerId, containerId, ownerId,
                                     timedObjectPrimaryKey, initialExpiration,
                                     intervalDuration, null, timerConfig);
        return;
    }

    public static void testMigrate(String fromOwnerId) {

        EJBTimerService ejbTimerService = getEJBTimerService();
        ejbTimerService.migrateTimers(fromOwnerId);

    }

    // XXX Called by TimerState via a static call
    public static BaseContainer getContainer(long containerId) {
        return EjbContainerUtilImpl.getInstance().getContainer(containerId);
    }

    // Called by TimerWelcomeServlet
    public Set findActiveNonPersistentTimersOwnedByThisServer() {
        EJBTimerService ejbTimerService = getEJBTimerService();
        return ejbTimerService.getActiveTimerIdsByThisServer();
    }

}
