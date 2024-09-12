# CompositeBaaS
<highlevel description>

## TODO
### Must Do / Critical:
- **Split/Merge**
    - mergeText currently receives an array of arrays due to how CompositeBaaS generates workflows. Each function works with multiple input files, this means the loop collects those collections and returns them as a collection of collections.
- **Parallelize wf**
    - we did parallelize, although hardcoded and not really pretty ✓
- **handle lambda limitations** appropriately (file sizes, durations)
- **regions**
    - intelligent selection?
    - option to set region? (CLI)
    - fix hardcoded region in lambdas
- **analyse** 
    - should filter fileNames result by type and format
- **Language codes**: AWS uses the format xx-XX (almost?) everywhere. We use xx 

### Important / High Priority:
- **credentials**
- **Directory structure and Output files**
    - CompositeBaaS output files
    - temp output workflow
    - output workflow
    - function directory structure
- **tests**

### Optional / Low Priority:
- **debug-flag**
    - use it to enable / disable verbose console output
- **Comments**
    - add comments (especially to models)
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
- **generate config.xml**?
- **service functions**
    - We could potentially send text directly.