package com.project.cloud.controller;

import com.project.cloud.model.Task;
import com.project.cloud.model.User;
import com.project.cloud.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping(value = "/user")
public class UserController {

    private final Logger LOG = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @RequestMapping(value = "/create", method = RequestMethod.POST)
    public User createNewUser(@RequestBody User user) {
        List<User> users = getAllUsers();
        int numOfUsers = users.size();
        if (numOfUsers == 0) {
            user.setUserId("1");
        } else {
            user.setUserId(String.valueOf(numOfUsers + 1));
        }
        List<Task> tasks = user.getTasks();
        if(tasks.size() != 0) {
            for(int i=0; i<tasks.size(); i++) {
                createNewTask(tasks.get(i), user.getUserId());
            }
        }
        LOG.info("Saving user {}", user.getName());
        return userRepository.save(user);
    }

    @RequestMapping(value = "/{userId}", method = RequestMethod.GET)
    public User getUser(@PathVariable String userId) {
        LOG.info("Getting user with ID: {}.", userId);
        return userRepository.findById(userId).get();
    }

    @RequestMapping(value = "", method = RequestMethod.GET)
    public List<User> getAllUsers() {
        LOG.info("Getting all users...");
        return userRepository.findAll();
    }

    @RequestMapping(value = "/delete/{userId}", method = RequestMethod.DELETE)
    public void deleteUser(@PathVariable String userId) {
        LOG.info("Finding user with ID: {}.", userId);
        User user = userRepository.findById(userId).get();
        if (user != null) {
            LOG.info("Deleting user with name {}..", user.getName());
            userRepository.delete(getUser(userId));
        } else {
            LOG.warn("User with userId {} not found!", userId);
        }
    }

    @RequestMapping(value = "/task/create/{userId}", method = RequestMethod.PUT)
    public User createNewTask(@RequestBody Task task, @PathVariable String userId) {

        User user = getUser(userId);
        if (user != null) {

            LOG.info("Adding task to the user with userId: {}.", userId);
            HashMap<String, Task> tasks = getAllTasks();

            List<String> tasksById = new ArrayList<>(tasks.keySet());
            Collections.sort(tasksById);
            int size = tasksById.size();
            String lastKeyValue = "0";
            for ( String key : tasks.keySet() ) {
                if(key.equals(tasksById.get(size-1))) {
                    lastKeyValue = key;
                    break;
                }
            }

            if(tasks.size() == 0) {
                task.setTaskId("1");
            } else {
                task.setTaskId(String.valueOf(Integer.parseInt(lastKeyValue) + 1));
            }
            task.setTaskCreatedBy(user.getName());
            task.setTaskCreationDate(java.time.LocalDate.now().toString());
            user.getTasks().add(task);
            return userRepository.save(user);
        } else {
            LOG.warn("Error user doesn't exist");
        }
        return null;
    }

    @RequestMapping(value = "/task/delete/{taskId}", method = RequestMethod.DELETE)
    public void deleteTask(@PathVariable String taskId) {

        List<User> users = getAllUsers();
        for(int i=0; i< users.size();i++) {
            List<Task> tasks = users.get(i).getTasks();
            for(int j=0;j<tasks.size();j++) {
                if (tasks.get(j).getTaskId().equals(taskId)) {
                    LOG.info("Deleting task with taskID: {} for user with userID: {}.", taskId, users.get(i).getUserId());
                    tasks.remove(tasks.get(j));
                    userRepository.save(users.get(i));
                }
            }
        }
    }

    @CrossOrigin
    @RequestMapping(value = "/tasks/{userId}", method = RequestMethod.GET)
    public List<Task> getAllTasksOfUser(@PathVariable String userId) {
        LOG.info("Getting all tasks of user with ID: {}.", userId);
        User user = userRepository.findById(userId).get();
        return user.getTasks();
    }

    private HashMap<String, Task> getAllTasks() {
        LOG.info("Getting all tasks of all the users");
        List<User> users = getAllUsers();
        HashMap<String, Task> tasks = new HashMap<>();
        for(int i=0; i<users.size();i++) {
            List<Task> taskList = users.get(i).getTasks();
            if(taskList.size() == 0) {
                LOG.info("No tasks for the user with userId: {}.", users.get(i).getUserId());
            } else {
                LOG.info("Getting all tasks for the user with userId: {}", users.get(i).getUserId());
                for(int j=0; j<taskList.size();j++) {
                    tasks.put(taskList.get(j).getTaskId(), taskList.get(j));
                }
            }
        }
        return tasks;
    }

    @RequestMapping(value = "/task/edit", method = RequestMethod.PUT)
    public void editTask(@RequestBody Task task) {
        List<User> users = getAllUsers();

        HashMap<String, Task> allTasks = getAllTasks();

        Task taskToEdit = null;

        for(String key : allTasks.keySet()) {
            if(key.equals(task.getTaskId())) {
                taskToEdit = allTasks.get(key);
            }
        }

        if(taskToEdit != null) {
            for (int i = 0; i < users.size(); i++) {
                List<Task> tasks = users.get(i).getTasks();
                for (int j = 0; j < tasks.size(); j++) {
                    if (tasks.get(j).getTaskId().equals(taskToEdit.getTaskId())) {
                        tasks.set(j, taskToEdit);
                        userRepository.save(users.get(i));
                    }
                }
            }
        } else {
            LOG.warn("Task not found!");
        }
    }

    @RequestMapping(value = "/task/{taskId}",method = RequestMethod.GET)
    public Task getTaskDetails(@PathVariable String taskId) {
        HashMap<String, Task> tasks = getAllTasks();

        LOG.info("Finding Task...");
        for(String key : tasks.keySet()) {
            Task task = tasks.get(key);
            if(taskId.equals(task.getTaskId())) {
                LOG.info("Task found!");
                return task;
            }
        }

        LOG.warn("Task with taskId {} not found!!", taskId);
        return null;
    }

    @RequestMapping(value = "/task/{taskId}/share/{userId}", method = RequestMethod.PUT)
    public void shareTask(@PathVariable String taskId, @PathVariable String userId) {
        User user = getUser(userId);
        if (user != null) {
            for(int i=0; i<user.getTasks().size();i++) {
                if(user.getTasks().get(i).getTaskId().equals(taskId)) {
                    LOG.warn("Task already exist!!");
                    return;
                }
            }

            Task task = getTaskDetails(taskId);
            if(task!=null) {
                LOG.info("Sharing task with user: {}.", user.getName());
                user.getTasks().add(getTaskDetails(taskId));
                userRepository.save(user);
            }
        } else {
            LOG.warn("User not found!!");
        }
    }
}