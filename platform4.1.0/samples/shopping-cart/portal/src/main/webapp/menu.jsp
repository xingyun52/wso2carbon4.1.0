<%@page import="com.acme.shoppingcart.portal.ProductsClient" %>
<%@ page import="com.acme.shoppingcart.portal.product.types.Category" %>
<%
    ProductsClient client = new ProductsClient();
    Category[] categories = client.listProductCategories();
%>
<a class="show-cart" onclick="toggleCart(this)" id="showCartIcon"></a>

<div class="navigation">
    <ul id="topnav">
        <li>
            <a href="index.jsp" class="home">Home</a>
        </li>
        <li>
            <a class="products">Products</a>

            <div style="opacity: 0; display: none;" class="sub">
                <ul>
                    <% if (categories != null) { %>
                    <% for (Category category : categories) {%>
                    <li>
                        <a href="view-category.jsp?category=<%= category.getCategoryName() %>">
                            <%= category.getCategoryName() %>
                        </a>
                    </li>
                    <% } %>
                    <% } %>
                </ul>
            </div>
        </li>
    </ul>
</div>
<div id="cartData" class="cartData" style="display:none"></div>
<form action="checkout-start.jsp" id="checkoutForm" method="post">
    <input type="hidden" id="cart" name="cart"/>
</form>