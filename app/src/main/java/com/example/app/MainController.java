package com.example.app;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import java.io.File;
import java.util.*;

@Controller
public class MainController {
    @Autowired
    private MongoTemplate mongoTemplate;
    private String defaultPath = "src/main/resources/static/";

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public ModelAndView index(@RequestParam(value = "cite", required = false, defaultValue = "200") int cite) {

//        String filename = "test5.svg";
        List<Node> nodes = getCiteGte(cite);
//        String svg = genSvgFromNodes(nodes, filename);
        ModelAndView mv = new ModelAndView("index");

//        mv.addObject("svg_main", svg);
        mv.addObject("nodes", nodes);
        mv.addObject("view", viewBox(nodes));
        mv.addObject("cite", cite);
        mv.addObject("num", nodes.size());
        return mv;
    }

    @RequestMapping(value = "/subgraph", method = RequestMethod.GET)
    public ModelAndView subgraph(@RequestParam("nid") int nid) {
//        String filename = "" + nid + ".svg";
        List<Node> nodes = getNodeCommunity(nid, 50);
//            genSvgFromNodes(nodes, filename);
        ModelAndView mv = new ModelAndView("subgraph");
        mv.addObject("nodes", nodes);
        mv.addObject("view", viewBox(nodes));
        return mv;
    }

    private String viewBox(List<Node> nodes) {
        float xmin = 0, xmax = 0, ymin = 0, ymax = 0;
        for (Node node : nodes) {
            float x = node.x;
            float y = node.y;
            if (x < xmin) xmin = x;
            if (x > xmax) xmax = x;
            if (y < ymin) ymin = y;
            if (y > ymax) ymax = y;
        }
        return "" + xmin + " " + ymin + " " + (xmax - xmin) + " " + (ymax - ymin);
    }

    private boolean exists(String filename) {
        return new File(defaultPath + filename).exists();
    }

    private List<Node> getNodeCommunity(int nid, int limit) {
        Node node = mongoTemplate.findOne(Query.query(Criteria.where("_id").is(nid)), Node.class);
        int range = 100;
        List<Node> ans;
        Query query = Query.query(Criteria.where("cid").is(node.cid));
        query.addCriteria(Criteria.where("citation").gte(limit));
        ans = mongoTemplate.find(query, Node.class);
        System.out.println(ans.size());
        return ans;
    }

    private List<Node> getCiteGte(int limit) {
        List<Node> ans;
        Query query = Query.query(Criteria.where("citation").gte(limit));
        query.addCriteria(Criteria.where("x").exists(true));
        ans = mongoTemplate.find(query, Node.class);
        System.out.println(ans.size());
        return ans;
    }

    private String genSvgFromNodes(List<Node> nodes, String fileName) {
        SVG svg = new SVG();
        Map<Integer, Node> m = new HashMap<>();
        for (Node node : nodes) m.put(node.id, node);
        for (Node node : nodes) {
            svg.addNode(node);
            if (node.citation >= 500)
                for (int tar : node.citedBy)
                    if (m.containsKey(tar))
                        svg.addEdge(node, m.get(tar));
        }
//        svg.exportTo("src/main/resources/static/" + fileName);
        return svg.toString();
    }
}
