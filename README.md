# CompositeBaaS
<highlevel description>

### TODO
- **Split/Merge**
    - add layer deps to ontology entries ✓
    - selectively upload layers (check servicePath for dependencies)
    - selectively add layers to lambdas (check servicePath for dependencies again)
    - test splitting video and merging text (overlap, artifacts) 
- **Parallelize wf**
    - encode parallel section in servicePath (ServicePathElement: GPT Chat *Refactor CLI Argument Handling*)
    - decide how to parallelize (nested vs seq)
- **handle lambda limitations** appropriately (file sizes, durations)
- **service functions**
    - We could potentially send text directly.
    - functions should have some kindof configuration params like available memory, timeout in ontology.
- **regions**
    - intelligent selection?
    - option to set region? (CLI)
    - fix hardcoded region in lambdas
- **tests**
- **debug-flag**
    - provide settable flag ✓
    - use it to enable / disable verbose console output
- **creadentials**
- **analyse** 
    - should filter fileNames result by type and format
    - some dataIns are duplicated
- **Directory structure and Output files**
    - CompositeBaaS output files
    - temp output workflow
    - output workflow
    - function directory structure
- **Comments**
    - add comments (especially to models)
- **layers** are not deleted correctly, because we look for layers with prefix compositebaas-, but leayers dont have this prefix
- **Language codes**: AWS uses the format xx-XX (almost?) everywhere. We use xx 

### Maybe TODO
- **Multi-Provider FCs**
    - PathFindingService:
        - check, which providers can be used (credentials present?)
        - only consider functions, who's provider we can use
    - DeploymentService: 
        - check, which providers we actually need 
        - set up each environment
        - upload service functions to their respective providers
    - FcGenerationService:
        - none
    - Misc:
        - service path needs to contain more than just a function name
- **generate config.xml** for apollo?