/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
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

package org.jboss.msc.test.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.jboss.msc.txn.BasicTransaction;
import org.jboss.msc.txn.CommitListener;
import org.jboss.msc.txn.Committable;
import org.jboss.msc.txn.Executable;
import org.jboss.msc.txn.ExecuteContext;
import org.jboss.msc.txn.InvalidTransactionStateException;
import org.jboss.msc.txn.PrepareListener;
import org.jboss.msc.txn.Revertible;
import org.jboss.msc.txn.AbortListener;
import org.jboss.msc.txn.RollbackListener;
import org.jboss.msc.txn.TaskController;
import org.jboss.msc.txn.TransactionController;
import org.jboss.msc.txn.TransactionRevertedException;
import org.jboss.msc.txn.Validatable;
import org.junit.After;
import org.junit.Before;

/**
 * Test base providing some utility methods.
 * 
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public abstract class AbstractTransactionTest {

    protected static final TransactionController txnController = TransactionController.createInstance();
    protected ThreadPoolExecutor defaultExecutor;

    @Before
    public void setUp() throws Exception {
        defaultExecutor = newExecutor(8, true);
    }

    @After
    public void tearDown() throws Exception {
        defaultExecutor.shutdown();
        try {
            defaultExecutor.awaitTermination(60, TimeUnit.SECONDS);
        } catch (final InterruptedException ignored) {}
        assertTrue(defaultExecutor.getQueue().size() == 0);
    }
    
    protected static ThreadPoolExecutor newExecutor(final int maximumPoolSize) {
        return newExecutor(maximumPoolSize, false);
    }

    protected static ThreadPoolExecutor newExecutor(final int maximumPoolSize, final boolean prestartCoreThreads) {
        final ThreadPoolExecutor executor = new ThreadPoolExecutor(maximumPoolSize, maximumPoolSize, 0L, TimeUnit.DAYS, new LinkedBlockingQueue<Runnable>());
        if (prestartCoreThreads) {
            executor.prestartAllCoreThreads();
        }
        return executor;
    }

    protected static <T> TaskController<T> newTask(final BasicTransaction transaction, final Executable<T> e, final Validatable v, final Revertible r, final Committable c, final TaskController<?>... dependencies) {
        return txnController.newTask(transaction, e).addDependencies(dependencies).setValidatable(v).setRevertible(r).setCommittable(c).release();
    }

    protected static <T> TaskController<T> newTask(final ExecuteContext<?> ctx, final Executable<T> e, final Validatable v, final Revertible r, final Committable c, final TaskController<?>... dependencies) {
        return ctx.newTask(e).addDependencies(dependencies).setValidatable(v).setRevertible(r).setCommittable(c).release();
    }

    protected static void prepare(BasicTransaction transaction, PrepareListener<BasicTransaction> listener) {
        txnController.prepare(transaction, listener);
    }

    protected static boolean canCommit(BasicTransaction transaction) {
        return txnController.canCommit(transaction);
    }

    protected static void commit(BasicTransaction transaction, CommitListener<BasicTransaction> listener) {
        txnController.commit(transaction, listener);
    }
    
    protected static void abort(BasicTransaction transaction, AbortListener<BasicTransaction> listener) {
        txnController.abort(transaction, listener);
    }

    protected static void rollback(BasicTransaction transaction, RollbackListener<BasicTransaction> listener) {
        txnController.rollback(transaction, listener);
    }

    protected BasicTransaction newTransaction() {
        assertNotNull(defaultExecutor);
        return txnController.create(defaultExecutor);
    }

    protected BasicTransaction newTransaction(final Executor executor) {
        assertNotNull(executor);
        return txnController.create(executor);
    }

    protected static void assertCalled(final TestTask task) {
        assertNotNull(task);
        assertTrue("Task " + task + " was not called", task.wasCalled());
    }

    protected static void assertNotCalled(final TestTask task) {
        assertNotNull(task);
        assertFalse("Task " + task + " was called", task.wasCalled());
    }

    protected static void assertCallOrder(final TestTask firstTask, final TestTask secondTask) {
        assertCalled(firstTask);
        assertCalled(secondTask);
        assertTrue("Task " + firstTask + " have been called after " + secondTask, firstTask.getCallTime() <= secondTask.getCallTime());
    }

    protected static void assertCallOrder(final TestTask firstTask, final TestTask secondTask, final TestTask... otherTasks) {
        assertCallOrder(firstTask, secondTask);
        if (otherTasks != null && otherTasks.length > 0) {
            TestTask previousTask = secondTask;
            for (final TestTask currentTask : otherTasks) {
                assertCallOrder(previousTask, currentTask);
                previousTask = currentTask;
            }
        }
    }

    protected static void assertPrepared(final BasicTransaction transaction) {
        assertPrepared(transaction, true);
    }

    protected static void assertPrepared(final BasicTransaction transaction, final boolean committable) {
        assertNotNull(transaction);
        assertEquals(committable, txnController.canCommit(transaction));
        try {
            txnController.prepare(transaction, null);
            fail("Cannot call prepare() on prepared transaction");
        } catch (final InvalidTransactionStateException expected) {
        } catch (final TransactionRevertedException abortedException) {
            assertFalse("Unexpected exception: " + abortedException, committable);
        }
        try {
            txnController.rollback(transaction, null);
            fail("Cannot call rollback() on prepared transaction");
        } catch (final TransactionRevertedException expected) {
        } catch (final InvalidTransactionStateException expected) {
        }
    }

    protected static void assertAborted(final BasicTransaction transaction) {
        assertNotNull(transaction);
        assertFalse(txnController.canCommit(transaction));
        try {
            txnController.prepare(transaction, null);
            fail("Cannot call prepare() on aborted transaction");
        } catch (final TransactionRevertedException expected) {
        }
        try {
            txnController.commit(transaction, null);
            fail("Cannot call commit() on aborted transaction");
        } catch (final TransactionRevertedException expected) {
        }
        try {
            txnController.abort(transaction, null);
            fail("Cannot call abort() on aborted transaction");
        } catch (final TransactionRevertedException expected) {
        }
        try {
            txnController.rollback(transaction, null);
            fail("Cannot call rollback() on aborted transaction");
        } catch (final InvalidTransactionStateException expected) {
        }
        assertTrue(transaction.isTerminated());
    }

    protected static void assertRolledBack(final BasicTransaction transaction) {
        assertNotNull(transaction);
        assertFalse(txnController.canCommit(transaction));
        try {
            txnController.prepare(transaction, null);
            fail("Cannot call prepare() on rolled back transaction");
        } catch (final TransactionRevertedException expected) {
        }
        try {
            txnController.commit(transaction, null);
            fail("Cannot call commit() on rolled back transaction");
        } catch (final TransactionRevertedException expected) {
        }
        try {
            txnController.abort(transaction, null);
            fail("Cannot call abort() on rolled back transaction");
        } catch (final TransactionRevertedException expected) {
        }
        try {
            txnController.rollback(transaction, null);
            fail("Cannot call rollback() on rolled back transaction");
        } catch (final TransactionRevertedException expected) {
        }
        assertTrue(transaction.isTerminated());
    }

    protected static void assertCommitted(final BasicTransaction transaction) {
        assertNotNull(transaction);
        assertFalse(txnController.canCommit(transaction));
        try {
            txnController.prepare(transaction, null);
            fail("Cannot call prepare() on committed transaction");
        } catch (final InvalidTransactionStateException expected) {
        }
        try {
            txnController.commit(transaction, null);
            fail("Cannot call commit() on committed transaction");
        } catch (final InvalidTransactionStateException expected) {
        }
        try {
            txnController.abort(transaction, null);
            fail("Cannot call abort() on committed transaction");
        } catch (final InvalidTransactionStateException expected) {
        }
        try {
            txnController.rollback(transaction, null);
            fail("Cannot call rollback() on committed transaction");
        } catch (final InvalidTransactionStateException expected) {
        }
        assertTrue(transaction.isTerminated());
    }

    protected static void prepare(final BasicTransaction transaction) throws InterruptedException {
        prepare(transaction, true);
    }

    protected static void prepare(final BasicTransaction transaction, boolean committable) throws InterruptedException {
        assertNotNull(transaction);
        final PrepareCompletionListener prepareListener = new PrepareCompletionListener();
        txnController.prepare(transaction, prepareListener);
        prepareListener.awaitCompletion();
        assertPrepared(transaction, committable);
    }

    protected static void commit(final BasicTransaction transaction) throws InterruptedException {
        assertNotNull(transaction);
        final CommitCompletionListener commitListener = new CommitCompletionListener();
        txnController.commit(transaction, commitListener);
        commitListener.awaitCompletion();
        assertCommitted(transaction);
    }

    protected static void rollback(final BasicTransaction transaction) throws InterruptedException {
        assertNotNull(transaction);
        final RollbackCompletionListener rollbackListener = new RollbackCompletionListener();
        txnController.rollback(transaction, rollbackListener);
        rollbackListener.awaitCompletion();
        assertRolledBack(transaction);
    }

    protected static void abort(final BasicTransaction transaction) throws InterruptedException {
        assertNotNull(transaction);
        final AbortCompletionListener abortListener = new AbortCompletionListener();
        txnController.abort(transaction, abortListener);
        abortListener.awaitCompletion();
        assertAborted(transaction);
    }

    protected static void prepareAndRollbackFromListener(final BasicTransaction transaction) throws InterruptedException {
        assertNotNull(transaction);
        final AbortingListener transactionListener = new AbortingListener(txnController);
        txnController.prepare(transaction, transactionListener);
        transactionListener.awaitAbort();
        assertAborted(transaction);
    }

    protected static void prepareAndCommitFromListener(final BasicTransaction transaction) throws InterruptedException {
        assertNotNull(transaction);
        final CommittingListener transactionListener = new CommittingListener(txnController);
        txnController.prepare(transaction, transactionListener);
        transactionListener.awaitCommit();
        assertCommitted(transaction);
    }

}
