package hello;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.*;

@Controller
public class IndexController {

    RunnableDemo runnableDemo;
    ExecutorService executorService;
    IndexAndSearch indexAndSearch;
    Index index;
    Model model;

    @Autowired
    private ApplicationContext appContext;

    @GetMapping("/")
    public String indexForm(Model model) {
        this.model = model;
        index = new Index();
        model.addAttribute("index", index);

        System.out.println("/index (GET)");

        return "index";
    }

    @GetMapping("/stop")
    public String indexStop(@ModelAttribute Index index) throws InterruptedException {
        //index.setRunning(false);
        // Attempts to stop all actively executing tasks, halts the processing of waiting tasks,
        // and returns a list of the tasks that were awaiting execution.
        //executorService.awaitTermination(1L, TimeUnit.SECONDS);
        executorService.shutdownNow();
        //this.index.setKeywords(null);
        //System.out.print("Keywords controller: " + index.getKeywords());
        System.out.println("/stop (GET)");

        return "url-error";

    }

    @PostMapping("/")
    public String indexSubmit(@ModelAttribute Index index, BindingResult result) {

        List<String> res = null;
        indexAndSearch = new IndexAndSearch(index);

        executorService = Executors.newFixedThreadPool(1);
        Future future = executorService.submit(indexAndSearch);

        try {
            res = (List) future.get();
        } catch (RuntimeException e ) {
            e.printStackTrace();
            try {
                indexAndSearch.getLuceneIndex().close();
                System.out.println("Closing writer");
            } catch (IOException e1) {
                e1.printStackTrace();
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
            try {
                indexAndSearch.getLuceneIndex().close();
                System.out.println("Closing writer");
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        System.out.println("Res: " + res);
        String template = null;

        /*
        if (res.size() == 10 && res != null) {
            index.setKeywords(res);
            System.out.println("Keywords controller!!! : " + index.getKeywords());
            template = "keywords";
        }
        */
        // if any errors, re-render the user info edit form
        if (res != null && (res.isEmpty() || res.size() < 10)) {
            //template = "index :: info-form";
            System.out.println("Res is empty!");
            this.index = new Index();
            template = "stop-error";

        }
        else if (res != null && res.size() == 10) {
            index.setKeywords(res);
            System.out.println("Keywords controller!!! : " + index.getKeywords());
            template = "keywords";
        }

        // If res == null (when invalid url)
        else if (res == null) {
            System.out.println("Invalid url!");
            this.index = new Index();
            template = "url-error";
        }

        System.out.println("/index (POST)");

        return template;
    }

}