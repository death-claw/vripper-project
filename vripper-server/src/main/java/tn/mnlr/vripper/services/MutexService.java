package tn.mnlr.vripper.services;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class MutexService {

    private final Map<String, ReentrantLock> postLock = new ConcurrentHashMap<>();

    public synchronized void createPostLock(String postId) {
        if (!postLock.containsKey(postId)) {
            postLock.put(postId, new ReentrantLock());
        }
    }

    public void removePostLock(String postId) {
        postLock.remove(postId);
    }

    public ReentrantLock getPostLock(String postId) {
        return postLock.get(postId);
    }
}
