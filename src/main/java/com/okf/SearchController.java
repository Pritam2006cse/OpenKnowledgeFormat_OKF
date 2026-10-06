package com.okf;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173")
public class SearchController {

    private final SearchService searchService;
    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }
    @GetMapping("/search")
    public List<SearchResultDTO> search(@RequestParam String q) {
        return searchService.search(q)
                .stream()
                .map(result ->
                        new SearchResultDTO(
                                result.getDocument().getId(),
                                result.getDocument().getTitle(),
                                result.getScore()
                        )
                )
                .toList();
    }
}