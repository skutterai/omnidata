# Role-Based Access Control in Skutter

This guide explains how to use role-based access control (RBAC) in your Skutter application.

## Roles

Skutter supports the following roles:

1. **PLATFORM_OWNER**: Full read/write access to all APIs
2. **PLATFORM_VIEWER**: Read-only access to all APIs
3. **PROJECT_ADMIN**: Read/write access to specific projects they're assigned to
4. **PROJECT_VIEWER**: Read-only access to specific projects they're assigned to

These roles are extracted from the JWT token's `app_metadata.skutter_role` claim.

## JWT Token Structure

Skutter expects the JWT token to have the following structure:

```json
{
  "sub": "user123",
  "email": "user@example.com",
  "app_metadata": {
    "skutter_role": "PROJECT_ADMIN"
  }
}
```

## Using Method-Level Security

Skutter provides custom security expressions for method-level security:

### Basic Role Checks

```java
// Require the user to have the PLATFORM_OWNER role
@PreAuthorize("isPlatformOwner()")

// Require the user to have the PLATFORM_VIEWER role or higher
@PreAuthorize("isPlatformViewer() or isPlatformOwner()")

// Require the user to have the PROJECT_ADMIN role
@PreAuthorize("isProjectAdmin()")

// Require the user to have the PROJECT_VIEWER role or higher
@PreAuthorize("isProjectViewer() or isProjectAdmin()")
```

### Platform-Level Access

```java
// Check if the user has platform-wide access
@PreAuthorize("hasPlatformAccess()")

// Check if the user has write access to the platform
@PreAuthorize("hasPlatformWriteAccess()")

// Check if the user has read access to the platform
@PreAuthorize("hasPlatformReadAccess()")
```

## Example Usage

```java
@RestController
@RequestMapping("/api/platform")
public class PlatformController {

    @GetMapping
    @PreAuthorize("hasPlatformAccess()")
    public List<Resource> getAllResources() {
        // Return all resources (only accessible to platform-wide roles)
        // ...
    }

    @PostMapping
    @PreAuthorize("hasPlatformWriteAccess()")
    public Resource createResource(@RequestBody Resource resource) {
        // Create a resource (only accessible to users with write access)
        // ...
    }
}
```

## Accessing User Details in Code

You can access the user details in your code:

```java
@Service
public class ResourceService {
    
    public Resource getResource(String resourceId) {
        // Get the authenticated user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (auth != null && auth.getPrincipal() instanceof SupabaseUserDetails) {
            SupabaseUserDetails user = (SupabaseUserDetails) auth.getPrincipal();
            
            // Check user's roles
            if (user.hasPlatformAccess()) {
                // User has platform-wide access
            } else {
                // User has limited access
            }
        }
        
        // ...
    }
}
```

## Configuring JWT Claims

The JWT claims are configured in the `application.yml` file:

```yaml
skutter:
  security:
    jwt:
      role-claim: app_metadata.skutter_role
      user-id-claim: sub
```

## Project-Specific Access Control

For project-specific access control, you'll need to implement your own authorization logic outside of the JWT token. You can create a service that maps users to projects, and use it in your controllers:

```java
@Service
public class ProjectAuthorizationService {
    // Inject your project-user mapping repository or service
    private final ProjectUserRepository projectUserRepository;
    
    public boolean hasProjectAccess(String userId, String projectId) {
        // Check if the user has access to the project
        return projectUserRepository.hasAccess(userId, projectId);
    }
    
    public boolean hasProjectWriteAccess(String userId, String projectId) {
        // Check if the user has write access to the project
        return projectUserRepository.hasWriteAccess(userId, projectId);
    }
}

@RestController
@RequestMapping("/api/projects")
public class ProjectController {
    private final ProjectAuthorizationService authService;
    
    @GetMapping("/{projectId}")
    public Project getProject(@PathVariable String projectId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        SupabaseUserDetails user = (SupabaseUserDetails) auth.getPrincipal();
        
        // Platform users can access any project
        if (user.hasPlatformAccess()) {
            // Allow access
        } else {
            // Check project-specific access
            if (!authService.hasProjectAccess(user.getUserId(), projectId)) {
                throw new AccessDeniedException("User does not have access to this project");
            }
        }
        
        // Return the project
        // ...
    }
} 