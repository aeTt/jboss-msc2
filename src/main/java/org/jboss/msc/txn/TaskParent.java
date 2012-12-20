/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012 Red Hat, Inc., and individual contributors
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

package org.jboss.msc.txn;

/**
 * Internal interface for task parent operations.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
interface TaskParent {

    /**
     * Indicate that a child's execution has finished.
     *
     * @param child the child
     */
    void childExecutionFinished(TaskChild child);

    /**
     * Indicate that a child's validation has finished.
     *
     * @param child the child
     */
    void childValidationFinished(TaskChild child);

    /**
     * Indicate that a child's rollback has finished.
     *
     * @param child the child
     */
    void childRollbackFinished(TaskChild child);

    /**
     * Indicate that a child's commit has finished.
     *
     * @param child the child
     */
    void childCommitFinished(TaskChild child);

    /**
     * Get the transaction implementation for this parent.
     *
     * @return the transaction implementation
     */
    TransactionImpl getTransaction();
}
