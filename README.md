# Quick Start Guide - Pawnshop Management System

## Step-by-Step Setup Instructions

### 1. Prerequisites Installation

#### Install JDK 17
```bash
# Check if Java is installed
java -version

# Should show Java 17 or higher
# If not, download from: https://adoptium.net/
```

#### Install MySQL 8
```bash
# On Ubuntu/Debian
sudo apt update
sudo apt install mysql-server

# On macOS (using Homebrew)
brew install mysql

# On Windows: Download installer from https://dev.mysql.com/downloads/mysql/
```

#### Install Apache Tomcat 10.1
```bash
# Download from: https://tomcat.apache.org/download-10.cgi
# Extract to a directory (e.g., /opt/tomcat or C:\tomcat)

# On Linux/Mac - Set environment variable
export CATALINA_HOME=/opt/tomcat

# On Windows - Set environment variable
set CATALINA_HOME=C:\tomcat
```

### 2. Create Project Structure

```bash
mkdir -p pawnshop-management/src/main/java/com/pawnshop/{config,dao,dto,filter,model,service,servlet}
mkdir -p pawnshop-management/src/main/resources
mkdir -p pawnshop-management/src/main/webapp/WEB-INF
mkdir -p pawnshop-management/src/test/java/com/pawnshop/service
cd pawnshop-management
```

### 3. Copy All Files

Place the files in their respective directories:

```
pawnshop-management/
├── build.gradle
├── settings.gradle
├── src/main/
│   ├── java/com/pawnshop/
│   │   ├── config/DatabaseConfig.java
│   │   ├── dao/AccountDAO.java
│   │   ├── dto/
│   │   │   ├── AccountRegistrationDTO.java
│   │   │   ├── AccountResponseDTO.java
│   │   │   ├── LoginRequestDTO.java
│   │   │   └── ApiResponse.java
│   │   ├── filter/AuthenticationFilter.java
│   │   ├── model/Account.java
│   │   ├── service/AccountService.java
│   │   └── servlet/
│   │       ├── AccountServlet.java
│   │       ├── AuthServlet.java
│   │       └── AuthServletJSP.java
│   ├── resources/
│   │   ├── database.properties
│   │   └── logback.xml
│   └── webapp/
│       ├── WEB-INF/web.xml
│       ├── index.html
│       ├── login.jsp
│       ├── dashboard.jsp
│       └── accounts.jsp
```

### 4. Setup Database

```sql
-- 1. Create database
CREATE DATABASE pawnshop CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 2. Use the database
USE pawnshop;

-- 3. Run the DDL script (create all tables from the provided DDL)

-- 4. Create admin account
INSERT INTO account (username, password_hash, role, is_active) 
VALUES ('admin', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5tdcMMih.a/Je', 'admin', TRUE);

-- 5. Verify
SELECT * FROM account;
```

### 5. Configure Database Connection

Edit `src/main/resources/database.properties`:

```properties
db.url=jdbc:mysql://localhost:3306/pawnshop?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
db.username=root
db.password=YOUR_MYSQL_PASSWORD
```

### 6. Build the Project

```bash
# On Linux/Mac
./gradlew clean build war

# On Windows
gradlew.bat clean build war

# WAR file will be created at: build/libs/pawnshop.war
```

### 7. Deploy to Tomcat

#### Option A: Manual Deployment
```bash
# Copy WAR to Tomcat webapps directory
cp build/libs/pawnshop.war $CATALINA_HOME/webapps/

# Start Tomcat
$CATALINA_HOME/bin/startup.sh  # Linux/Mac
# OR
%CATALINA_HOME%\bin\startup.bat  # Windows
```

#### Option B: IDE Deployment (IntelliJ IDEA)
1. Open project in IntelliJ IDEA
2. File → Project Structure → Artifacts → Add → Web Application: Archive → From modules...
3. Run → Edit Configurations → Add New → Tomcat Server → Local
4. Configure Tomcat home directory
5. Add deployment artifact
6. Run the server

### 8. Access the Application

Open browser and navigate to:
```
http://localhost:8080/pawnshop/
```

**Default Admin Credentials:**
- Username: `admin`
- Password: `admin123`

### 9. Test the Application

#### Test Login (JSP)
1. Go to: `http://localhost:8080/pawnshop/login.jsp`
2. Enter credentials
3. Should redirect to dashboard

#### Test REST API (using cURL)
```bash
# Login via API
curl -X POST http://localhost:8080/pawnshop/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' \
  -c cookies.txt

# Get current user
curl -X GET http://localhost:8080/pawnshop/api/auth/current \
  -b cookies.txt

# Register new account (admin only)
curl -X POST http://localhost:8080/pawnshop/api/accounts/register \
  -H "Content-Type: application/json" \
  -b cookies.txt \
  -d '{"username":"testuser","password":"test123","role":"employee"}'

# Get all accounts
curl -X GET http://localhost:8080/pawnshop/api/accounts/all \
  -b cookies.txt
```

### 10. Verify Functionality

#### Admin Functions:
1. ✅ Login as admin
2. ✅ View dashboard
3. ✅ Navigate to Accounts page
4. ✅ Register new user (employee/customer/admin)
5. ✅ View all accounts list
6. ✅ Deactivate an account

#### Security Tests:
1. ✅ Try accessing `/accounts.jsp` without login → Should redirect to login
2. ✅ Login as employee, try accessing `/accounts.jsp` → Should deny access
3. ✅ Logout and verify session is cleared

## Troubleshooting

### Issue: Port 8080 already in use
```bash
# Find process using port 8080
lsof -i :8080  # Linux/Mac
netstat -ano | findstr :8080  # Windows

# Change Tomcat port in $CATALINA_HOME/conf/server.xml
# Look for: <Connector port="8080" ...
# Change to: <Connector port="8081" ...
```

### Issue: Database connection failed
```bash
# Check MySQL is running
sudo systemctl status mysql  # Linux
mysql.server status  # Mac
# Windows: Check Services

# Test connection
mysql -u root -p
# Enter password and verify you can connect
```

### Issue: ClassNotFoundException for jakarta.servlet
- **Cause**: Using Tomcat 9.x instead of 10.x
- **Solution**: Download and use Tomcat 10.1.x or higher

### Issue: BCrypt password doesn't match
- **Generate new password hash:**
```java
// Run this in a simple Java program
import org.mindrot.jbcrypt.BCrypt;
public class HashGenerator {
    public static void main(String[] args) {
        String password = "admin123";
        String hash = BCrypt.hashpw(password, BCrypt.gensalt(12));
        System.out.println(hash);
    }
}
```

### Issue: Gradle build fails
```bash
# Clean gradle cache
./gradlew clean --refresh-dependencies

# Check Java version
java -version

# Ensure JAVA_HOME is set
echo $JAVA_HOME  # Linux/Mac
echo %JAVA_HOME%  # Windows
```

### Issue: JSP pages not rendering
- Check JSP files are in `src/main/webapp/` not `src/main/resources/`
- Verify no syntax errors in JSP
- Check Tomcat logs: `$CATALINA_HOME/logs/catalina.out`

## Logs Location

- **Application Logs**: `logs/pawnshop.log` (created by logback)
- **Tomcat Logs**: `$CATALINA_HOME/logs/`
    - `catalina.out` - Standard output
    - `localhost.log` - Application errors
    - `manager.log` - Manager application logs

## Testing Checklist

- [ ] Database connection successful
- [ ] Login page loads
- [ ] Admin can login
- [ ] Dashboard displays after login
- [ ] Admin can access Accounts page
- [ ] Non-admin cannot access Accounts page
- [ ] Registration form works
- [ ] New account appears in accounts list
- [ ] Logout works correctly
- [ ] REST API endpoints respond correctly

## Next Steps

After successful setup:

1. **Implement remaining entities**: Customer, Employee, Product, PawnContract
2. **Create additional JSP pages**: Complete CRUD for all entities
3. **Add validation**: Client-side and server-side
4. **Implement search/filtering**: For all list pages
5. **Add pagination**: For large datasets
6. **Email notifications**: Contract expiry reminders
7. **Reports & Analytics**: Revenue charts, statistics

## Support Resources

- Jakarta EE Servlet Spec: https://jakarta.ee/specifications/servlet/5.0/
- Jakarta EE Pages Spec: https://jakarta.ee/specifications/pages/3.0/
- Tomcat 10 Documentation: https://tomcat.apache.org/tomcat-10.1-doc/
- MySQL Documentation: https://dev.mysql.com/doc/
- BCrypt Documentation: https://github.com/jeremyh/jBCrypt

## Common Commands Reference

```bash
# Build
./gradlew clean build

# Generate WAR only
./gradlew war

# Start Tomcat
$CATALINA_HOME/bin/startup.sh

# Stop Tomcat
$CATALINA_HOME/bin/shutdown.sh

# View Tomcat logs (live)
tail -f $CATALINA_HOME/logs/catalina.out

# Restart after changes
./gradlew war && cp build/libs/pawnshop.war $CATALINA_HOME/webapps/ && $CATALINA_HOME/bin/shutdown.sh && sleep 2 && $CATALINA_HOME/bin/startup.sh
```

---