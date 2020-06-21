package com.example.app;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

@Document(collection = "node")
public class Node {
    @Id
    public int id;
    public int citation;
    public float x, y;
    public float r;
    public String title;
    public int cid;
    public List<Integer> citedBy;

    public Node(int id, int citation, float x, float y) {
        this.id = id;
        this.citation = citation;
        this.x = x;
        this.y = y;
    }

    @Override
    public String toString() {
        return "" + id;
    }
}
