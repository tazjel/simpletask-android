/**
 * This file is part of Todo.txt Touch, an Android app for managing your todo.txt file (http://todotxt.com).
 *
 * Copyright (c) 2009-2012 Todo.txt contributors (http://todotxt.com)
 *
 * LICENSE:
 *
 * Todo.txt Touch is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 2 of the License, or (at your option) any
 * later version.
 *
 * Todo.txt Touch is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with Todo.txt Touch.  If not, see
 * <http://www.gnu.org/licenses/>.
 *
 * @author Todo.txt contributors <todotxt@yahoogroups.com>
 * @license http://www.gnu.org/licenses/gpl.html
 * @copyright 2009-2012 Todo.txt contributors (http://todotxt.com)
 */
package nl.mpcjanssen.simpletask.task;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;

import nl.mpcjanssen.simpletask.TodoApplication;
import nl.mpcjanssen.simpletask.TodoException;
import nl.mpcjanssen.simpletask.util.TaskIo;
import nl.mpcjanssen.simpletask.util.Util;
import android.util.Log;


/**
 * A task repository for interacting with the local file system
 * 
 * @author Tim Barlotta
 */
public class LocalFileTaskRepository {
	private  final String TAG = LocalFileTaskRepository.class
			.getSimpleName();
	final File TODO_TXT_FILE;
	final File DONE_TXT_FILE;
	private final TaskBag.Preferences preferences;
    private final TodoApplication m_app;

    public LocalFileTaskRepository(TodoApplication app, File todo,  TaskBag.Preferences preferences) {
        this.m_app = app;
		this.preferences = preferences;
        this.TODO_TXT_FILE = todo;
        this.DONE_TXT_FILE = new File(todo.getParentFile(), "done.txt");
	}

    public File getTodoTxtFile() {
        return TODO_TXT_FILE;
    }

	public void init() {
		try {
			if (!TODO_TXT_FILE.exists()) {
				Util.createParentDirectory(TODO_TXT_FILE);
				TODO_TXT_FILE.createNewFile();
			}
		} catch (IOException e) {
			throw new TodoException("Error initializing LocalFile", e);
		}
	}

	public ArrayList<Task> load() {
		init();
		if (!TODO_TXT_FILE.exists()) {
			Log.w(TAG, TODO_TXT_FILE.getAbsolutePath() + " does not exist!");
			throw new TodoException(TODO_TXT_FILE.getAbsolutePath()
					+ " does not exist!");
		} else {
			try {
				return TaskIo.loadTasksFromFile(TODO_TXT_FILE, preferences);
			} catch (IOException e) {
				throw new TodoException("Error loading from local file", e);
			}
		}
	}

	public void store(ArrayList<Task> tasks) {
        m_app.stopWatching();
		TaskIo.writeToFile(tasks, TODO_TXT_FILE,
				preferences.isUseWindowsLineBreaksEnabled());
        m_app.startWatching();
	}

	public void archive(ArrayList<Task> tasks,  List<Task> tasksToArchive) {
        m_app.stopWatching();
		boolean windowsLineBreaks = preferences.isUseWindowsLineBreaksEnabled();

		ArrayList<Task> archivedTasks = new ArrayList<Task>(tasks.size());
		ArrayList<Task> remainingTasks = new ArrayList<Task>(tasks.size());

        for (Task task : tasks) {
            if (tasksToArchive!=null) {
                // Archive selected tasks
                if (tasksToArchive.indexOf(task)!=-1) {
                    archivedTasks.add(task);
                } else {
                    remainingTasks.add(task);
                }
            } else {
                // Archive completed tasks
                if (task.isCompleted()) {
                    archivedTasks.add(task);
                } else {
                    remainingTasks.add(task);
                }
            }
        }

		// append completed tasks to done.txt
		TaskIo.writeToFile(archivedTasks, DONE_TXT_FILE, true,
				windowsLineBreaks);

		// write incomplete tasks back to todo.txt
		// TODO: remove blank lines (if we ever add support for
		// PRESERVE_BLANK_LINES)
		TaskIo.writeToFile(remainingTasks, TODO_TXT_FILE, false,
				windowsLineBreaks);
        m_app.startWatching();
	}

	public void loadDoneTasks(File file) {
		Util.renameFile(file, DONE_TXT_FILE, true);
	}

    public void removeDoneFile() {
        DONE_TXT_FILE.delete();
    }
	public boolean todoFileModifiedSince(Date date) {
		long date_ms = 0l;
		if (date != null) {
			date_ms = date.getTime();
		}
		return date_ms < TODO_TXT_FILE.lastModified();
	}

	public boolean doneFileModifiedSince(Date date) {
		long date_ms = 0l;
		if (date != null) {
			date_ms = date.getTime();
		}
		return date_ms < DONE_TXT_FILE.lastModified();
	}
}
