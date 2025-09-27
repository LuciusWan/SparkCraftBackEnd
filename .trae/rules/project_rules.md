# Cursor Rules for SparkCraftBackEnd - AI Image Generation Platform

## Project Overview
SparkCraftBackEnd is a Spring Boot 3.x AI image generation platform that integrates with Alibaba's Qwen AI service for generating images. The project follows a layered architecture with clear separation of concerns and implements design patterns for extensibility.

## Technology Stack
- **Framework**: Spring Boot 3.5.5
- **Language**: Java 17+
- **AI Integration**: Alibaba DashScope SDK (Qwen AI)
- **Build Tool**: Maven
- **Database**: MySQL with MyBatis-Flex ORM
- **Cloud Storage**: Alibaba Cloud OSS
- **API Documentation**: Knife4j (Swagger)
- **Utilities**: Hutool, Lombok
- **Testing**: JUnit 5, Spring Boot Test

## Project Structure
```
src/main/java/com/lucius/dachuangbackend/
├── ai/                    # AI service integration
├── annotation/            # Custom annotations
├── aop/                   # Aspect-oriented programming
├── common/                # Common components (BaseResponse, ResultUtils)
├── config/                # Configuration classes
├── constant/              # Constants definition
├── controller/            # REST controllers
├── dto/                   # Data transfer objects
├── entity/                # Database entities
├── enums/                 # Enumerations
├── exception/             # Custom exceptions
├── factory/               # Factory classes
├── generate/              # Code generation utilities
├── mapper/                # Data access layer (MyBatis)
├── properties/            # Configuration properties
├── service/               # Business services
│   └── impl/              # Service implementations
├── strategy/              # Strategy pattern implementations
│   └── impl/              # Strategy implementations
├── test/                  # Test utilities
├── utils/                 # Utility classes
└── vo/                    # View objects
```

## Design Patterns Used
1. **Strategy Pattern**: ImageGenerationStrategy for different AI image generation methods
2. **Factory Pattern**: ImageGenerationStrategyFactory for strategy selection
3. **Layered Architecture**: Controller → Service → Mapper → Database
4. **Template Method Pattern**: Common response handling patterns

## Coding Standards

### General Guidelines
- Use meaningful variable and method names in English
- Follow Java naming conventions (camelCase for methods/variables, PascalCase for classes)
- Add comprehensive JavaDoc comments for public methods and classes
- Use Lombok annotations to reduce boilerplate code (@Data, @Slf4j, @Getter, @Setter)
- Implement proper error handling with custom exceptions

### Spring Boot Specific
- Use `@RestController` for REST API controllers
- Use `@Service` for business service classes
- Use `@Component` for general Spring components
- Inject dependencies using `@Resource` or `@Autowired`
- Use `@Slf4j` for logging instead of System.out.println
- Follow RESTful API conventions for controllers
- Use `@RequestMapping` with appropriate HTTP methods

### Database and Persistence
- Use MyBatis-Flex for ORM operations
- Entity classes should use `@Table` annotation
- Follow the naming convention: Entity → Mapper → Service → Controller
- Use proper transaction management with `@Transactional`
- Always handle database exceptions appropriately

### AI Integration
- Use strategy pattern for different AI service integrations
- Implement proper error handling for AI API calls
- Use configuration classes for AI service parameters
- Follow the established pattern: Request → Strategy Selection → AI Call → Result Processing

### File Operations and Cloud Storage
- Use Hutool's `FileUtil` for file operations
- Always specify UTF-8 encoding for text files
- Implement proper error handling for OSS operations
- Use the established ImageProcessingService for file uploads

## Code Examples

### Controller Implementation
```java
@RestController
@RequestMapping("/api/paper")
@Slf4j
public class PaperController {
    
    @Resource
    private PaperService paperService;
    
    @PostMapping("/make")
    public BaseResponse<PaperMakeVO> makePaper(@RequestBody PaperMakeRequest request, HttpServletRequest httpRequest) {
        try {
            User loginUser = userService.getLoginUser(httpRequest);
            PaperMakeVO result = paperService.makePaper(request, loginUser);
            return ResultUtils.success(result);
        } catch (BusinessException e) {
            log.error("Paper creation failed", e);
            return ResultUtils.error(e.getCode(), e.getMessage());
        }
    }
}
```

### Service Implementation
```java
@Service
@Slf4j
public class PaperServiceImpl implements PaperService {
    
    @Resource
    private ImageGenerationStrategyFactory strategyFactory;
    
    @Resource
    private PaperMapper paperMapper;
    
    @Override
    @Transactional
    public PaperMakeVO makePaper(PaperMakeRequest request, User user) {
        // Validate request
        validatePaperRequest(request);
        
        // Get strategy and generate image
        ImageGenerationStrategy strategy = strategyFactory.getStrategy(request);
        ImageGenerationResult result = strategy.generateImage(request);
        
        // Process and save
        Paper paper = buildPaper(request, result, user);
        paperMapper.insert(paper);
        
        return buildPaperMakeVO(paper);
    }
}
```

### Strategy Pattern Implementation
```java
public interface ImageGenerationStrategy {
    ImageGenerationResult generateImage(PaperMakeRequest request);
    String getStrategyName();
    boolean supports(PaperMakeRequest request);
}

@Component
public class QwenImageGenerationStrategy implements ImageGenerationStrategy {
    
    @Override
    public ImageGenerationResult generateImage(PaperMakeRequest request) {
        // Implement Qwen AI image generation logic
        return result;
    }
    
    @Override
    public String getStrategyName() {
        return "qwen-image";
    }
    
    @Override
    public boolean supports(PaperMakeRequest request) {
        // Define support conditions
        return true;
    }
}
```

### Exception Handling
```java
@Getter
public class BusinessException extends RuntimeException {
    private final int code;
    
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }
    
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }
}
```

## Error Handling
- Use `BusinessException` for business logic errors
- Use `ErrorCode` enum for standardized error codes
- Log errors appropriately with context using `@Slf4j`
- Return meaningful error messages using `BaseResponse` and `ResultUtils`
- Handle AI API failures gracefully with retry mechanisms

## Response Format
- Always use `BaseResponse<T>` for API responses
- Use `ResultUtils.success(data)` for successful responses
- Use `ResultUtils.error(ErrorCode)` for error responses
- Maintain consistent response structure across all endpoints

## Configuration Management
- Use `application.yml` for configuration
- Support environment-specific configurations (local, dev, prod)
- Use `@ConfigurationProperties` for complex configurations
- Externalize sensitive information using environment variables

## Testing Guidelines
- Write unit tests for all service methods
- Use `@SpringBootTest` for integration tests
- Mock external dependencies using `@MockBean`
- Test both success and failure scenarios
- Use descriptive test method names

## Security Considerations
- Validate all input parameters
- Use proper authentication and authorization
- Don't log sensitive information (API keys, passwords)
- Follow OWASP security guidelines
- Implement rate limiting for AI API calls

## Performance Guidelines
- Use appropriate caching strategies
- Implement proper connection pooling for external services
- Handle large file uploads efficiently
- Use async processing where appropriate
- Monitor and optimize database queries

## API Documentation
- Use Knife4j for API documentation
- Add comprehensive `@ApiOperation` and `@ApiParam` annotations
- Provide example requests and responses
- Document error codes and their meanings
- Keep documentation up to date with code changes

## When Adding New Features
1. Follow existing architectural patterns
2. Use appropriate design patterns (Strategy, Factory, etc.)
3. Add comprehensive tests
4. Update API documentation
5. Consider backward compatibility
6. Use existing utilities and patterns where possible
7. Follow the established error handling patterns

## Common Patterns to Follow
- Use the Strategy pattern for different processing types
- Implement Factory pattern for strategy selection
- Apply proper separation of concerns
- Keep controllers thin, put business logic in services
- Use consistent naming conventions
- Follow the established request/response patterns

## File Naming Conventions
- Controllers: `*Controller.java`
- Services: `*Service.java` (interface) and `*ServiceImpl.java` (implementation)
- Entities: `*.java` (simple class names like `User.java`, `Paper.java`)
- DTOs: `*Request.java`, `*Response.java`, `*VO.java`
- Exceptions: `*Exception.java`
- Enums: `*Enum.java` or descriptive names like `ErrorCode.java`
- Strategies: `*Strategy.java` (interface) and `*StrategyImpl.java` (implementation)
- Factories: `*Factory.java`
- Mappers: `*Mapper.java`

## Database Conventions
- Use snake_case for database table and column names
- Use camelCase for Java entity field names
- Follow the naming pattern: `user_account` (DB) → `userAccount` (Java)
- Use appropriate data types and constraints
- Include audit fields (createTime, updateTime) in entities

## Logging Guidelines
- Use `@Slf4j` annotation for logging
- Log important business operations
- Use appropriate log levels (DEBUG, INFO, WARN, ERROR)
- Include relevant context in log messages
- Don't log sensitive information

## AI Integration Best Practices
- Implement proper retry mechanisms for AI API calls
- Handle rate limiting and quota management
- Validate AI service responses
- Implement fallback strategies for AI service failures
- Monitor AI service usage and costs

Remember to maintain consistency with the existing codebase and follow the established patterns when implementing new features. The project emphasizes clean architecture, proper error handling, and extensibility through design patterns.