// main.js
document.addEventListener("DOMContentLoaded", function() {
  // Xử lý navbar khi cuộn
  window.addEventListener("scroll", function () {
    const navbar = document.querySelector(".navbar");
    if (navbar) {
      if (window.scrollY > 50) {
        navbar.classList.add("scrolled");
      } else {
        navbar.classList.remove("scrolled");
      }
    }
  });

  // Swiper initialization
  document.querySelectorAll('.movie-poster-slider').forEach((section, index) => {
    const swiperContainer = section.querySelector('.mySwiper');
    if (swiperContainer) {
      const swiper = new Swiper(swiperContainer, {
        slidesPerView: "auto",
        spaceBetween: 20,
        navigation: {
          nextEl: section.querySelector('.swiper-button-next'),
          prevEl: section.querySelector('.swiper-button-prev'),
        },
      });
    }
  });
});