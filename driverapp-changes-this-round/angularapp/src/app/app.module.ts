import { BrowserModule } from '@angular/platform-browser';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { HTTP_INTERCEPTORS, HttpClientModule } from '@angular/common/http';
import { APP_INITIALIZER, NgModule } from '@angular/core';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { AddAdminComponent } from './components/add-admin/add-admin.component';
import { AdminViewDriversComponent } from './components/admin-view-drivers/admin-view-drivers.component';
import { AdminnavComponent } from './components/adminnav/adminnav.component';
import { AdminviewfeedbackComponent } from './components/adminviewfeedback/adminviewfeedback.component';
import { AdminviewrequestsComponent } from './components/adminviewrequests/adminviewrequests.component';
import { CustomerRequestComponent } from './components/customer-request/customer-request.component';
import { CustomernavComponent } from './components/customernav/customernav.component';
import { CustomerpostfeedbackComponent } from './components/customerpostfeedback/customerpostfeedback.component';
import { CustomerviewdriverComponent } from './components/customerviewdriver/customerviewdriver.component';
import { CustomerviewfeedbackComponent } from './components/customerviewfeedback/customerviewfeedback.component';
import { CustomerviewrequestedComponent } from './components/customerviewrequested/customerviewrequested.component';
import { DriverManagementComponent } from './components/driver-management/driver-management.component';
import { ErrorComponent } from './components/error/error.component';
import { HomePageComponent } from './components/home-page/home-page.component';
import { LoginComponent } from './components/login/login.component';
import { SignupComponent } from './components/signup/signup.component';
import { SkeletonComponent } from './components/skeleton/skeleton.component';
import { HttpErrorInterceptor } from './services/http-error.interceptor';
import { AuthService } from './services/auth.service';

@NgModule({
  declarations: [
    AppComponent,
    AddAdminComponent,
    AdminViewDriversComponent,
    AdminnavComponent,
    AdminviewfeedbackComponent,
    AdminviewrequestsComponent,
    CustomerRequestComponent,
    CustomernavComponent,
    CustomerpostfeedbackComponent,
    CustomerviewdriverComponent,
    CustomerviewfeedbackComponent,
    CustomerviewrequestedComponent,
    DriverManagementComponent,
    ErrorComponent,
    HomePageComponent,
    LoginComponent,
    SignupComponent,
    SkeletonComponent
  ],
  imports: [
    BrowserModule,
    FormsModule,
    ReactiveFormsModule,
    HttpClientModule,
    AppRoutingModule
  ],
  providers: [
    { provide: HTTP_INTERCEPTORS, useClass: HttpErrorInterceptor, multi: true },
    // Before the first page is shown, ask the server whether the login stored in the browser is still valid.
    {
      provide: APP_INITIALIZER,
      useFactory: (authService: AuthService) => () => authService.validateSession().toPromise(),
      deps: [AuthService],
      multi: true
    }
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
