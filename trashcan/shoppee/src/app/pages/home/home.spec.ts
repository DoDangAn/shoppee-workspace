// home.spec.ts
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { Home } from './home';

describe('Home', () => {
  let component: Home;
  let fixture: ComponentFixture<Home>;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [Home],
      imports: [HttpClientTestingModule]
    }).compileComponents();

    fixture = TestBed.createComponent(Home);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should fetch home data from API', () => {
    const mockData = {
      loggedUser: 'testuser',
      totalMovies: 100,
      totalCategories: 10,
      totalUsers: 50,
      loggedInUser: { id: '1', username: 'testuser' },
      category: [{ id: '1', name: 'Electronics' }],
      products: [{ id: '1', name: 'Product 1', price: 99.99, releaseDate: '2023-01-01', viewCount: 1000 }],
      price: [],
      score: [],
      release: []
    };

    component.ngOnInit();
    const req = httpMock.expectOne('http://localhost:8080/api/home?field=viewCount&field2=rate&field3=releaseDate');
    expect(req.request.method).toBe('GET');
    req.flush(mockData);

    expect(component.homeData).toEqual(mockData);
  });
});