export enum Role {
    USER = 'USER',
    ADMIN = 'ADMIN'
  }
  
  export interface User {
    id: string;
    email: string;
    role: Role;
    createdAt: string;
  }
  
  export interface AuthResponse {
    accessToken: string;
  }
  
  export interface LoginRequest {
    email: string;
    password: string;
  }
  
  export interface RegisterRequest {
    email: string;
    password: string;
  }