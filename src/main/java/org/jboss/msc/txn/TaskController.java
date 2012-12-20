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
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public abstract class TaskController<T> implements TaskTarget {

    TaskController() {}

    /**
     * Get the transaction associated with this controller.
     *
     * @return the transaction associated with this controller
     */
    public abstract Transaction getTransaction();

    public abstract T getResult() throws IllegalStateException;

    public abstract <T> TaskBuilder<T> newTask(Executable<T> task) throws IllegalStateException;

    public abstract TaskBuilder<Void> newTask() throws IllegalStateException;
}
